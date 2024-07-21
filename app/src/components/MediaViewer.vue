<template>
  <q-dialog
    :model-value="!!src"
    backdrop-filter="blur(4px)"
    @hide="$emit('close')"
  >
    <img
      :src="src"
      alt="Media"
      width="80%"
      height="80%"
      style="object-fit: contain"
    />
  </q-dialog>
</template>

<script setup>
defineOptions({
  name: "MediaViewer",
});
defineProps({
  src: {
    required: true,
    type: String,
  },
});
const emit = defineEmits(["close"]);
</script>

<script>
export default {};
</script>
