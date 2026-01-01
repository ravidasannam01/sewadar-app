package com.rssb.application.util;

import com.rssb.application.dto.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ExportUtil {

    public static byte[] exportSewadarsToCSV(SewadarDashboardResponse response) throws IOException {
        StringBuilder csv = new StringBuilder();
        
        // Header
        csv.append("Zonal ID,First Name,Last Name,Mobile,Location,Profession,Joining Date,Languages,")
           .append("Total Programs,Total Days,BEAS Programs,BEAS Days,Non-BEAS Programs,Non-BEAS Days\n");
        
        // Data rows
        for (SewadarDashboardResponse.SewadarDashboardItem item : response.getSewadars()) {
            csv.append(escapeCSV(item.getZonalId().toString())).append(",")
               .append(escapeCSV(item.getFirstName())).append(",")
               .append(escapeCSV(item.getLastName())).append(",")
               .append(escapeCSV(item.getMobile())).append(",")
               .append(escapeCSV(item.getLocation())).append(",")
               .append(escapeCSV(item.getProfession())).append(",")
               .append(escapeCSV(item.getJoiningDate() != null ? item.getJoiningDate().toString() : "")).append(",")
               .append(escapeCSV(String.join("; ", item.getLanguages()))).append(",")
               .append(item.getTotalProgramsCount()).append(",")
               .append(item.getTotalDaysAttended()).append(",")
               .append(item.getBeasProgramsCount()).append(",")
               .append(item.getBeasDaysAttended()).append(",")
               .append(item.getNonBeasProgramsCount()).append(",")
               .append(item.getNonBeasDaysAttended()).append("\n");
        }
        
        return csv.toString().getBytes("UTF-8");
    }

    public static byte[] exportSewadarsToXLSX(SewadarDashboardResponse response) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Sewadars");
        
        // Create header style
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        // Create header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Zonal ID", "First Name", "Last Name", "Mobile", "Location", "Profession",
                           "Joining Date", "Languages", "Total Programs", "Total Days", "BEAS Programs",
                           "BEAS Days", "Non-BEAS Programs", "Non-BEAS Days"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Data rows
        int rowNum = 1;
        for (SewadarDashboardResponse.SewadarDashboardItem item : response.getSewadars()) {
            Row row = sheet.createRow(rowNum++);
            int colNum = 0;
            
            row.createCell(colNum++).setCellValue(item.getZonalId());
            row.createCell(colNum++).setCellValue(item.getFirstName());
            row.createCell(colNum++).setCellValue(item.getLastName());
            row.createCell(colNum++).setCellValue(item.getMobile());
            row.createCell(colNum++).setCellValue(item.getLocation());
            row.createCell(colNum++).setCellValue(item.getProfession());
            row.createCell(colNum++).setCellValue(item.getJoiningDate() != null ? item.getJoiningDate().toString() : "");
            row.createCell(colNum++).setCellValue(String.join("; ", item.getLanguages()));
            row.createCell(colNum++).setCellValue(item.getTotalProgramsCount());
            row.createCell(colNum++).setCellValue(item.getTotalDaysAttended());
            row.createCell(colNum++).setCellValue(item.getBeasProgramsCount());
            row.createCell(colNum++).setCellValue(item.getBeasDaysAttended());
            row.createCell(colNum++).setCellValue(item.getNonBeasProgramsCount());
            row.createCell(colNum++).setCellValue(item.getNonBeasDaysAttended());
        }
        
        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    public static byte[] exportSewadarsToPDF(SewadarDashboardResponse response) throws IOException {
        PDDocument document = new PDDocument();
        PDPage page = new PDPage();
        document.addPage(page);
        
        PDPageContentStream contentStream = new PDPageContentStream(document, page);
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
        contentStream.beginText();
        contentStream.newLineAtOffset(50, 750);
        contentStream.showText("Sewadars Report");
        contentStream.endText();
        
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
        float y = 720;
        float x = 50;
        
        // Header
        contentStream.beginText();
        contentStream.newLineAtOffset(x, y);
        contentStream.showText("Zonal ID | Name | Mobile | Location | Total Programs | Total Days");
        contentStream.endText();
        
        y -= 20;
        for (SewadarDashboardResponse.SewadarDashboardItem item : response.getSewadars()) {
            if (y < 50) {
                contentStream.close();
                page = new PDPage();
                document.addPage(page);
                contentStream = new PDPageContentStream(document, page);
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                y = 750;
            }
            
            String line = item.getZonalId() + " | " + item.getFirstName() + " " + item.getLastName() +
                         " | " + item.getMobile() + " | " + item.getLocation() + " | " +
                         item.getTotalProgramsCount() + " | " + item.getTotalDaysAttended();
            
            contentStream.beginText();
            contentStream.newLineAtOffset(x, y);
            contentStream.showText(line);
            contentStream.endText();
            y -= 15;
        }
        
        contentStream.close();
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        document.save(out);
        document.close();
        return out.toByteArray();
    }

    public static byte[] exportSewadarAttendanceToCSV(SewadarDetailedAttendanceResponse response) throws IOException {
        StringBuilder csv = new StringBuilder();
        
        csv.append("Sewadar: ").append(response.getSewadarName()).append(" (").append(response.getSewadarId()).append(")\n");
        csv.append("Mobile: ").append(response.getMobile()).append("\n\n");
        csv.append("Program ID,Program Title,Location,Date,Status\n");
        
        for (SewadarDetailedAttendanceResponse.AttendanceRecord record : response.getRecords()) {
            csv.append(record.getProgramId()).append(",")
               .append(escapeCSV(record.getProgramTitle())).append(",")
               .append(escapeCSV(record.getProgramLocation())).append(",")
               .append(record.getAttendanceDate().toString()).append(",")
               .append(record.getStatus()).append("\n");
        }
        
        return csv.toString().getBytes("UTF-8");
    }

    public static byte[] exportProgramAttendanceToCSV(ProgramDetailedAttendanceResponse response) throws IOException {
        StringBuilder csv = new StringBuilder();
        
        csv.append("Program: ").append(response.getProgramTitle()).append(" (").append(response.getProgramId()).append(")\n\n");
        
        // Header: Sewadar info + dates
        csv.append("Zonal ID,Name,Mobile,");
        for (java.time.LocalDate date : response.getProgramDates()) {
            csv.append(date.toString()).append(",");
        }
        csv.append("\n");
        
        // Data rows
        for (ProgramDetailedAttendanceResponse.SewadarAttendanceRow row : response.getSewadarRows()) {
            csv.append(row.getZonalId()).append(",")
               .append(escapeCSV(row.getSewadarName())).append(",")
               .append(escapeCSV(row.getMobile())).append(",");
            
            for (java.time.LocalDate date : response.getProgramDates()) {
                csv.append(row.getDateStatusMap().getOrDefault(date, "Absent")).append(",");
            }
            csv.append("\n");
        }
        
        return csv.toString().getBytes("UTF-8");
    }

    public static byte[] exportApplicationsToCSV(ApplicationDashboardResponse response) throws IOException {
        StringBuilder csv = new StringBuilder();
        
        csv.append("Application ID,Zonal ID,Name,Mobile,Status,Applied At\n");
        
        for (ApplicationDashboardResponse.ApplicationDashboardItem item : response.getApplications()) {
            csv.append(item.getApplicationId()).append(",")
               .append(item.getSewadarZonalId()).append(",")
               .append(escapeCSV(item.getSewadarName())).append(",")
               .append(escapeCSV(item.getMobile())).append(",")
               .append(item.getStatus()).append(",")
               .append(item.getAppliedAt() != null ? item.getAppliedAt().toString() : "").append("\n");
        }
        
        return csv.toString().getBytes("UTF-8");
    }

    private static String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}

