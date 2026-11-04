package com.restautomation.commons.utilities.readerhelpers;

import com.restautomation.commons.utilities.common.FileUtils;
import com.restautomation.commons.utilities.common.JsonUtils;
import com.restautomation.exceptions.FrameworkException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Reads CSV files (first row = header) into ordered maps or typed objects. */
public final class CsvFileReader {

    private CsvFileReader() {
    }

    public static List<Map<String, Object>> readAll(String path) {
        return readAll(path, ',');
    }

    public static List<Map<String, Object>> readAll(String path, char delimiter) {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setDelimiter(delimiter)
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build();
        try (Reader reader = new InputStreamReader(FileUtils.openStream(path), StandardCharsets.UTF_8);
             CSVParser parser = format.parse(reader)) {
            List<String> headers = parser.getHeaderNames().stream()
                    .map(header -> header.replace("﻿", ""))
                    .toList();
            List<Map<String, Object>> rows = new ArrayList<>();
            for (CSVRecord record : parser) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 0; i < headers.size(); i++) {
                    String value = i < record.size() ? record.get(i) : null;
                    row.put(headers.get(i), value == null || value.isEmpty() ? null : value);
                }
                rows.add(row);
            }
            return rows;
        } catch (IOException e) {
            throw new FrameworkException("Unable to read CSV file " + path, e);
        }
    }

    public static <T> List<T> readAs(String path, Class<T> type) {
        return JsonUtils.convertList(readAll(path), type);
    }

    public static List<Map<String, Object>> findRows(String path, String column, String value) {
        return readAll(path).stream()
                .filter(row -> Objects.equals(String.valueOf(row.get(column)), value))
                .toList();
    }
}
