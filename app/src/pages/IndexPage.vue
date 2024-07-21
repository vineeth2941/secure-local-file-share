<template>
  <q-page class="q-pa-lg flex q-gutter-sm">
    <template v-if="directory">
      <file-explorer
        :path="path"
        :directory="directory.directory"
        @explore="onExplore"
        @show="onShow"
      ></file-explorer>
    </template>
    <media-viewer :src="url" @close="url = ''"></media-viewer>
  </q-page>
</template>

<script setup>
import FileExplorer from "components/FileExplorer.vue";
import MediaViewer from "components/MediaViewer.vue";

defineOptions({
  name: "IndexPage",
});
</script>

<script>
import { myAxios } from "../util";
import { Loading } from "quasar";

export default {
  components: [FileExplorer, MediaViewer],
  data() {
    return {
      path: [],
      directory: null,
      url: "",
    };
  },
  methods: {
    async getDirectory(path) {
      Loading.show();
      const resp = await myAxios.get(`/api/files?path=${path.join("/")}`);
      this.directory = resp.data;
      Loading.hide();
    },
    async onExplore(path) {
      await this.getDirectory(path);
      this.path = path;
    },
    async onShow(path) {
      Loading.show();
      try {
        const resp = await myAxios.get(`/api/file?path=${path.join("/")}`);
        const base64String = btoa(resp.data);
        this.url = `data:${resp.headers["content-type"]};base64,${base64String}`;
        Loading.hide();
      } catch (error) {
        console.error(error);
        Loading.hide();
      }
    },
  },
  mounted() {
    this.getDirectory(this.path);
  },
};
</script>
