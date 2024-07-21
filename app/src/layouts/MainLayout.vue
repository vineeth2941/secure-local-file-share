<template>
  <q-layout view="lHh Lpr lFf">
    <q-header elevated>
      <q-toolbar class="bg-primary text-white">
        <q-avatar>
          <img src="/icons/favicon-32x32.png" alt="Secure Share" />
        </q-avatar>
        <q-toolbar-title> Secure Share </q-toolbar-title>
      </q-toolbar>
    </q-header>

    <q-page-container>
      <template v-if="setupCompleted">
        <router-view />
      </template>
    </q-page-container>
  </q-layout>
</template>

<script setup>
defineOptions({
  name: "MainLayout",
});
</script>

<script>
import axios from "axios";
import {
  myAxios,
  toBuffer,
  toString,
  encodeFromBuffer,
  decodeToBuffer,
} from "../util";
import { Loading } from "quasar";

export default {
  data() {
    return {
      setupCompleted: false,
      clientId: null,
      keyPair: null,
      publicKey: null,
      serverKey: null,
      sharedKey: null,
    };
  },
  methods: {
    getClientId() {
      const CLIENT_ID_KEY = "client-id";
      if (localStorage.getItem(CLIENT_ID_KEY)) {
        this.clientId = localStorage.getItem(CLIENT_ID_KEY);
      } else {
        this.clientId = crypto.randomUUID();
        localStorage.setItem(CLIENT_ID_KEY, this.clientId);
      }
      console.log("Client ID", this.clientId);
      return this.clientId;
    },
    async getClientKeyPair() {
      this.getClientId();

      const salt = await crypto.subtle.digest(
        "SHA-256",
        toBuffer(this.clientId)
      );
      const saltKey = await crypto.subtle.importKey(
        "raw",
        salt,
        { name: "HKDF" },
        false,
        ["deriveKey"]
      );

      const masterKey = await crypto.subtle.deriveKey(
        {
          name: "HKDF",
          salt: toBuffer(""),
          info: toBuffer("Master Key"),
          hash: "SHA-256",
        },
        saltKey,
        {
          name: "AES-CBC",
          length: 256,
        },
        false,
        ["encrypt", "decrypt"]
      );

      const clientKeyPair = localStorage.getItem("client-key-pair");
      if (clientKeyPair) {
        try {
          console.log(
            clientKeyPair,
            String.fromCharCode(...decodeToBuffer(clientKeyPair))
          );
          const decryptedKeyPair = await crypto.subtle.decrypt(
            { name: "AES-CBC", iv: new Uint8Array(16) },
            masterKey,
            decodeToBuffer(clientKeyPair)
          );
          console.log(decryptedKeyPair);
          const keyPairObject = JSON.parse(
            String.fromCharCode(...new Uint8Array(decryptedKeyPair))
          );
          console.log(keyPairObject);
          this.keyPair = {
            privateKey: await crypto.subtle.importKey(
              "pkcs8",
              decodeToBuffer(keyPairObject.privateKey),
              {
                name: "ECDH",
                namedCurve: "P-521",
              },
              true,
              ["deriveKey", "deriveBits"]
            ),
            publicKey: await crypto.subtle.importKey(
              "spki",
              decodeToBuffer(keyPairObject.publicKey),
              {
                name: "ECDH",
                namedCurve: "P-521",
              },
              true,
              []
            ),
          };
        } catch (error) {
          console.error(error);
          console.warn("Could not retrive key pair from cache");
        }
      }
      if (!this.keyPair) {
        this.keyPair = await crypto.subtle.generateKey(
          {
            name: "ECDH",
            namedCurve: "P-521",
          },
          true,
          ["deriveKey", "deriveBits"]
        );
        const keyPairJson = JSON.stringify({
          privateKey: encodeFromBuffer(
            await crypto.subtle.exportKey("pkcs8", this.keyPair.privateKey)
          ),
          publicKey: encodeFromBuffer(
            await crypto.subtle.exportKey("spki", this.keyPair.publicKey)
          ),
        });
        const encryptedKeyPair = await crypto.subtle.encrypt(
          { name: "AES-CBC", iv: new Uint8Array(16) },
          masterKey,
          toBuffer(keyPairJson)
        );
        console.log(
          new Uint8Array(encryptedKeyPair),
          String.fromCharCode(...new Uint8Array(encryptedKeyPair)),
          encodeFromBuffer(encryptedKeyPair),
          atob(encodeFromBuffer(encryptedKeyPair)),
          decodeToBuffer(encodeFromBuffer(encryptedKeyPair))
        );
        localStorage.setItem(
          "client-key-pair",
          encodeFromBuffer(encryptedKeyPair)
        );
      }

      this.publicKey = encodeFromBuffer(
        await crypto.subtle.exportKey("spki", this.keyPair.publicKey)
      );
      console.log("Public Key:", this.publicKey);
    },
    async getServerKey() {
      const resp = await axios.get("/api/public-key");
      const bytes = decodeToBuffer(resp.data);
      this.serverKey = await crypto.subtle.importKey(
        "spki",
        bytes,
        {
          name: "ECDH",
          namedCurve: "P-521",
        },
        false,
        []
      );
      console.log("Server Public Key", this.serverKey);
    },
    async generateSharedKey() {
      await this.getClientKeyPair();

      await this.getServerKey();

      this.sharedKey = await crypto.subtle.deriveKey(
        {
          name: "ECDH",
          public: this.serverKey,
        },
        this.keyPair.privateKey,
        {
          name: "AES-CBC",
          length: 256,
        },
        false,
        ["encrypt", "decrypt"]
      );
      console.log("Shared Key", this.sharedKey);
    },
    async requestInterceptor(config) {
      console.log("request", config);
      config.headers.set("public-key", this.publicKey);
      config.headers["Content-Type"] = "application/json";
      if (config.data) {
        config.data = encodeFromBuffer(
          await crypto.subtle.encrypt(
            { name: "AES-CBC", length: 256, iv: new Uint8Array(16) },
            this.sharedKey,
            toBuffer(JSON.stringify(config.data)) // do it after data serialization
          )
        );
      }
      return config;
    },
    async responseInterceptor(config) {
      // TODO: effective deserilization based on content type
      console.log("response", config);
      if (config.status >= 200 && config.status < 300) {
        const decryptedData = await crypto.subtle.decrypt(
          { name: "AES-CBC", length: 256, iv: new Uint8Array(16) },
          this.sharedKey,
          decodeToBuffer(config.data)
        );
        const data = toString(decryptedData);
        switch (config.headers["content-type"]) {
          case "application/json":
            config.data = JSON.parse(data);
            break;
          default:
            config.data = data;
            break;
        }
        console.log("Decrypted data", config.data);
      }
      return config;
    },
  },
  mounted() {
    Loading.show({});

    this.generateSharedKey()
      .then(() => {
        const oFetch = window.fetch;
        window.fetch = (...params) => {
          console.log(params);
          return oFetch(params);
        };
        this.requestInterceptorId = myAxios.interceptors.request.use(
          this.requestInterceptor
        );
        this.responseInterceptorId = myAxios.interceptors.response.use(
          this.responseInterceptor
        );

        this.setupCompleted = true;
        Loading.hide();
      })
      .catch(console.error);
  },
  async unmounted() {
    await alert("Removing interceptors");
    myAxios.interceptors.request.eject(this.requestInterceptorId);
    myAxios.interceptors.response.eject(this.responseInterceptorId);
    console.log("Removed interceptors");
  },
};
</script>
