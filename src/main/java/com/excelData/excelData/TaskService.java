package com.excelData.excelData;

import org.apache.poi.ss.usermodel.*;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public Flux<Task> processAndSave(FilePart filePart) {
        if (filePart == null) {
            return Flux.error(new IllegalArgumentException("Uploaded file part is null. Ensure Postman form-data key is 'file'."));
        }

        return DataBufferUtils.join(filePart.content())
                .map(dataBuffer -> dataBuffer.asInputStream(true))
                .flatMapMany(inputStream ->
                        Mono.fromCallable(() -> parseExcel(inputStream))
                                .subscribeOn(Schedulers.boundedElastic())
                                .flatMapMany(list -> (list != null && !list.isEmpty()) ? Flux.fromIterable(list) : Flux.empty())
                )
                .filter(Objects::nonNull)
                .buffer(500)
                .flatMap(taskRepository::saveAll);
    }

    private List<Task> parseExcel(InputStream inputStream) throws Exception {
        List<Task> tasks = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) return tasks;

            int headerRowIndex = -1;
            Map<String, Integer> headerMap = new HashMap<>();

            Map<Integer, String> rawHeaderIndexMap = new HashMap<>();

            for (int r = 0; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                for (Cell cell : row) {
                    String val = getCellValue(row, cell.getColumnIndex()).toLowerCase();
                    if (val.contains("project name") || val.contains("task name")) {
                        headerRowIndex = r;
                        break;
                    }
                }

                if (headerRowIndex != -1) {
                    Row hRow = sheet.getRow(headerRowIndex);
                    for (Cell cell : hRow) {
                        int colIdx = cell.getColumnIndex();
                        String rawColName = getCellValue(hRow, colIdx);
                        String normalizedColName = rawColName.toLowerCase().replaceAll("\\s+", "");

                        if (!normalizedColName.isEmpty()) {
                            headerMap.put(normalizedColName, colIdx);
                            rawHeaderIndexMap.put(colIdx, rawColName);
                        }
                    }
                    break;
                }
            }

            if (headerRowIndex == -1) {
                throw new IllegalArgumentException("Header row not found in Excel sheet.");
            }


            int projectCol = headerMap.getOrDefault("projectname", headerMap.getOrDefault("project", -1));
            int taskCol = headerMap.getOrDefault("taskname", headerMap.getOrDefault("task", -1));
            int assignedCol = headerMap.getOrDefault("assignedto", headerMap.getOrDefault("assigned", -1));
            int startCol = headerMap.getOrDefault("startdate", headerMap.getOrDefault("start", -1));
            int endCol = headerMap.getOrDefault("enddate", headerMap.getOrDefault("end", -1));
            int progressCol = headerMap.getOrDefault("progress", -1);


            Set<Integer> knownColumnIndices = new HashSet<>();
            if (projectCol != -1) knownColumnIndices.add(projectCol);
            if (taskCol != -1) knownColumnIndices.add(taskCol);
            if (assignedCol != -1) knownColumnIndices.add(assignedCol);
            if (startCol != -1) knownColumnIndices.add(startCol);
            if (endCol != -1) knownColumnIndices.add(endCol);
            if (progressCol != -1) knownColumnIndices.add(progressCol);

            // 2. Parse data rows
            for (int i = headerRowIndex + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String projectName = getCellValue(row, projectCol);
                String taskName = getCellValue(row, taskCol);
                String assignedTo = getCellValue(row, assignedCol);
                String startDateStr = getCellValue(row, startCol);
                String endDateStr = getCellValue(row, endCol);
                String progressStr = getCellValue(row, progressCol);

                // Skip blank rows
                if (projectName.isEmpty() && taskName.isEmpty()) continue;

                Task task = new Task();
                task.setProjectName(projectName);
                task.setTaskName(taskName);
                task.setAssignedTo(assignedTo);

                // Date parsing logic
                if (!startDateStr.isEmpty()) {
                    Cell cell = row.getCell(startCol);
                    if (cell != null && cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                        task.setStartDate(cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
                    } else {
                        try {
                            task.setStartDate(LocalDate.parse(startDateStr));
                        } catch (Exception ignored) {
                        }
                    }
                }

                if (!endDateStr.isEmpty()) {
                    Cell cell = row.getCell(endCol);
                    if (cell != null && cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                        task.setEndDate(cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
                    } else {
                        try {
                            task.setEndDate(LocalDate.parse(endDateStr));
                        } catch (Exception ignored) {
                        }
                    }
                }

                // Double parsing logic
                if (!progressStr.isEmpty()) {
                    try {
                        task.setProgress(Double.parseDouble(progressStr));
                    } catch (NumberFormatException ignored) {
                        task.setProgress(0.0);
                    }
                }

                // 3. Process Miscellaneous Columns
                StringJoiner miscJoiner = new StringJoiner("; ");
                for (Map.Entry<Integer, String> entry : rawHeaderIndexMap.entrySet()) {
                    int colIdx = entry.getKey();
                    String rawHeaderName = entry.getValue();

                    // Skip known primary columns
                    if (knownColumnIndices.contains(colIdx)) continue;

                    String cellValue = getCellValue(row, colIdx);
                    if (!cellValue.isEmpty()) {
                        miscJoiner.add(rawHeaderName + "=" + cellValue);
                    }
                }

                if (miscJoiner.length() > 0) {
                    task.setMiscellaneous(miscJoiner.toString());
                }

                tasks.add(task);
            }
        }

        return tasks;
    }

    private String getCellValue(Row row, int colIndex) {
        if (row == null || colIndex < 0) return "";
        Cell cell = row.getCell(colIndex);
        if (cell == null) return "";

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getDateCellValue().toString()
                    : String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }
}


//    private List<Task> parseExcel(InputStream inputStream) throws Exception {
//        List<Task> tasks = new ArrayList<>();
//
//        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
//            Sheet sheet = workbook.getSheetAt(0);
//
//            if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) {
//                return tasks;
//            }
//
//            Map<String, Integer> headerMap = new HashMap<>();
//            int headerRowIndex = -1;
//
//            // 1. Find and map the header row dynamically
//            for (int r = 0; r <= sheet.getLastRowNum(); r++) {
//                Row row = sheet.getRow(r);
//                if (row == null) continue;
//
//                for (Cell cell : row) {
//                    String cellValue = getCellValueAsString(cell).trim().toLowerCase();
//                    if (cellValue.contains("project name") || cellValue.contains("task name")) {
//                        headerRowIndex = r;
//                        break;
//                    }
//                }
//
//                if (headerRowIndex != -1) {
//                    Row hRow = sheet.getRow(headerRowIndex);
//                    for (Cell cell : hRow) {
//                        String columnName = getCellValueAsString(cell).trim().toLowerCase();
//                        if (!columnName.isEmpty()) {
//                            headerMap.put(columnName, cell.getColumnIndex());
//                        }
//                    }
//                    break;
//                }
//            }
//
//            if (headerRowIndex == -1) {
//                throw new IllegalArgumentException("Header row not found in Excel sheet.");
//            }
//
//            // 2. Parse Data Rows
//            for (int r = headerRowIndex + 1; r <= sheet.getLastRowNum(); r++) {
//                Row row = sheet.getRow(r);
//                if (row == null || isRowEmpty(row)) continue;
//
//                String projectName = getCellStringValue(row, headerMap, "project name");
//                String taskName = getCellStringValue(row, headerMap, "task name");
//                String assignedTo = getCellStringValue(row, headerMap, "assigned to");
//                int daysRequired = (int) getCellNumericValue(row, headerMap, "days required");
//                double progress = getCellNumericValue(row, headerMap, "progress");
//
//                LocalDate startDate = getCellDateValue(row, headerMap, "start date");
//                LocalDate endDate = getCellDateValue(row, headerMap, "end date");
//
//
//                if (assignedTo.startsWith("n ") || assignedTo.startsWith("n\t")) {
//                    taskName = taskName + "n";
//                    assignedTo = assignedTo.substring(1).trim();
//                }
//
//                if (startDate != null) {
//                    LocalDate calculatedEndDate = startDate.plusDays(daysRequired);
//                    if (endDate == null || endDate.isBefore(startDate) || !endDate.equals(calculatedEndDate)) {
//                        endDate = calculatedEndDate; // Fix invalid date (e.g. Row 18)
//                    }
//                }
//
//                // Build domain object
//                Task task = new Task();
//                task.setProjectName(projectName);
//                task.setTaskName(taskName);
//                task.setAssignedTo(assignedTo);
//                task.setStartDate(startDate);
//                task.setEndDate(endDate);
//                task.setProgress(progress);
//
//                tasks.add(task);
//            }
//        }
//
//        return tasks;
//    }
//
//    private String getCellStringValue(Row row, Map<String, Integer> headerMap, String columnName) {
//        Integer colIdx = headerMap.get(columnName);
//        if (colIdx == null) return "";
//        Cell cell = row.getCell(colIdx);
//        return getCellValueAsString(cell).trim();
//    }
//
//    private double getCellNumericValue(Row row, Map<String, Integer> headerMap, String columnName) {
//        Integer colIdx = headerMap.get(columnName);
//        if (colIdx == null) return 0;
//        Cell cell = row.getCell(colIdx);
//        if (cell == null) return 0;
//        if (cell.getCellType() == CellType.NUMERIC) {
//            return cell.getNumericCellValue();
//        } else if (cell.getCellType() == CellType.STRING) {
//            try {
//                return Double.parseDouble(cell.getStringCellValue().trim());
//            } catch (NumberFormatException e) {
//                return 0;
//            }
//        }
//        return 0;
//    }
//
//    private LocalDate getCellDateValue(Row row, Map<String, Integer> headerMap, String columnName) {
//        Integer colIdx = headerMap.get(columnName);
//        if (colIdx == null) return null;
//        Cell cell = row.getCell(colIdx);
//        if (cell == null) return null;
//
//        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
//            Date date = cell.getDateCellValue();
//            return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
//        } else if (cell.getCellType() == CellType.STRING) {
//            try {
//                return LocalDate.parse(cell.getStringCellValue().trim());
//            } catch (Exception e) {
//                return null;
//            }
//        }
//        return null;
//    }
//
//    private String getCellValueAsString(Cell cell) {
//        if (cell == null) return "";
//        return switch (cell.getCellType()) {
//            case STRING -> cell.getStringCellValue();
//            case NUMERIC -> DateUtil.isCellDateFormatted(cell) ? cell.getDateCellValue().toString() : String.valueOf(cell.getNumericCellValue());
//            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
//            case FORMULA -> cell.getCellFormula();
//            default -> "";
//        };
//    }
//
//    private boolean isRowEmpty(Row row) {
//        for (Cell cell : row) {
//            if (cell != null && cell.getCellType() != CellType.BLANK && !getCellValueAsString(cell).trim().isEmpty()) {
//                return false;
//            }
//        }
//        return true;
//    }
