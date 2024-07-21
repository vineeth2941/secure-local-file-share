import { Axios } from "axios";

export const myAxios = new Axios({});

export const toBuffer = (str) => {
  return new Uint8Array([...str].map((c) => c.charCodeAt(0)));
};

export const toString = (bytes) => {
  const batchSize = 1000;
  const arr = [...new Uint8Array(bytes)];
  let str = "";
  while (arr.length > 0) {
    str += String.fromCharCode(...arr.splice(0, batchSize));
  }
  return str;
};

export const encodeFromBuffer = (buffer) => {
  return btoa(toString(buffer));
};

export const decodeToBuffer = (encodedKey) => {
  return toBuffer(atob(encodedKey));
};
