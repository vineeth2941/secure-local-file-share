<template>
  <q-list bordered padding class="full-width rounded-borders text-primary">
    <q-item>
      <q-item-section>
        <q-breadcrumbs>
          <template
            v-for="(label, index) in [undefined].concat(path)"
            :key="label"
          >
            <q-breadcrumbs-el
              to="#"
              :icon="label ? undefined : 'home'"
              :label="label"
              @click="onBreadcrumbClick(index)"
            />
          </template>
        </q-breadcrumbs>
      </q-item-section>
    </q-item>
    <q-separator />
    <q-item
      clickable
      v-ripple
      v-for="file in directory"
      :key="file.name"
      @click="onFileClick(file)"
    >
      <q-item-section avatar>
        <q-avatar
          v-if="file.isDirectory"
          icon="folder"
          color="primary"
          text-color="white"
        />
        <q-avatar
          v-else
          icon="insert_drive_file"
          color="grey"
          text-color="white"
        />
      </q-item-section>
      <q-item-section>{{ file.name }}</q-item-section>
    </q-item>
  </q-list>
</template>

<script setup>
defineOptions({
  name: "FileExplorer",
});
defineProps({
  path: {
    required: true,
    type: Array,
  },
  directory: {
    required: true,
    type: {
      name: String,
      isDirectory: Boolean,
    },
  },
});
const emit = defineEmits(["explore", "show"]);
</script>

<script>
export default {
  methods: {
    onBreadcrumbClick(index) {
      this.$emit("explore", this.$props.path.slice(0, index));
    },
    onFileClick(file) {
      if (file.isDirectory) {
        this.$emit("explore", this.$props.path.concat(file.name));
      } else {
        this.$emit("show", this.$props.path.concat(file.name));
      }
    },
  },
};
</script>
