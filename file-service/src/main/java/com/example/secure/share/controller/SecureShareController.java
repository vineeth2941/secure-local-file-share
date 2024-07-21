package com.example.secure.share.controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.secure.share.annotation.IgnoreEncryption;
import com.example.secure.share.dto.FileDto;
import com.example.secure.share.exception.InvalidPathException;
import com.example.secure.share.service.CryptoService;

@RestController
public class SecureShareController {

	@Value("${folder-path}")
	private String folderPath;

	@Autowired
	private CryptoService cryptoService;

	@IgnoreEncryption
	@GetMapping("/public-key")
	public String getPublicKey() {
		return cryptoService.getPublicKey();
	}

	@GetMapping("/files")
	public FileDto requestFiles(@RequestParam(required = false, defaultValue = "") String path)
			throws FileNotFoundException {
		if (path.startsWith(".") || path.startsWith("/") || path.contains("/..")) {
			throw new InvalidPathException(path);
		}

		File directory = Paths.get(folderPath, path).toFile();
		if (!directory.canRead()) {
			throw new FileNotFoundException(path);
		}

		FileDto fileDto = new FileDto(directory);
		if (fileDto.getIsDirectory()) {
			fileDto.getDirectory().addAll(Stream.of(directory.listFiles()).filter(f -> !f.isHidden() && f.canRead())
					.map(FileDto::new).sorted((f1, f2) -> {
						if (f1.getIsDirectory() != f2.getIsDirectory()) {
							return f1.getIsDirectory() ? -1 : 1;
						}
						return f1.getName().compareToIgnoreCase(f2.getName());
					}).toList());
		}
		return fileDto;
	}

	@GetMapping("/file")
	public ResponseEntity<Resource> requestFile(@RequestParam String path) {
		if (path.startsWith(".") || path.startsWith("/") || path.contains("/..")) {
			throw new InvalidPathException(path);
		}

		File file = Paths.get(folderPath, path).toFile();
		if (!file.isFile()) {
			throw new InvalidPathException(path);
		}

		FileSystemResource resource = new FileSystemResource(file);
		MediaType mediaType = MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(mediaType);

		return new ResponseEntity<Resource>(resource, headers, HttpStatus.OK);
	}

}
