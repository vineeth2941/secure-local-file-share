package com.example.secure.share.dto;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileDto {

	private String name;
	private Boolean isDirectory;
	private List<FileDto> directory;

	public FileDto(File file) {
		name = file.getName();
		isDirectory = file.isDirectory();
		directory = isDirectory ? new ArrayList<>() : null;
	}

	public String getName() {
		return name;
	}

	public Boolean getIsDirectory() {
		return isDirectory;
	}

	public List<FileDto> getDirectory() {
		return directory;
	}

}
