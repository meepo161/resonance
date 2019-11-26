package ru.avem.resonance.logging

import org.apache.poi.openxml4j.exceptions.InvalidFormatException
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import ru.avem.resonance.Main
import ru.avem.resonance.db.model.Protocol
import ru.avem.resonance.model.Point
import ru.avem.resonance.utils.Toast
import ru.avem.resonance.utils.Utils.copyFileFromStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat

object Logging {

    const val TO_DESIRED_ROW = 0

    fun getTempWorkbook(protocol: Protocol): File? {
        return writeWorkbookToTempFile(protocol)
    }

    private fun writeWorkbookToTempFile(protocol: Protocol): File {
        clearDirectory()
        val sdf = SimpleDateFormat("dd_MM(HH-mm-ss)")
        val fileName = "protocol-" + sdf.format(System.currentTimeMillis()) + ".xlsx"

        val file = File("protocol", fileName)
        if (!writeWorkbookToFile(protocol, file)) {
            Toast.makeText("Произошла ошибка при попытке отображения протокола").show(Toast.ToastType.ERROR)
        }
        return file
    }

    private fun clearDirectory() {
        val directory = File("protocol")
        if (!directory.exists()) {
            directory.mkdir()
        } else if (directory.listFiles() != null) {
            for (child in directory.listFiles()!!) {
                child.delete()
            }
        }
    }

    fun writeWorkbookToFile(protocol: Protocol, file: File): Boolean {
        try {
            val out = convertProtocolToWorkbook(protocol)

            val fileOut = FileOutputStream(file)
            out.writeTo(fileOut)
            out.close()
            fileOut.close()
        } catch (e: IOException) {
            return false
        } catch (e: InvalidFormatException) {
            return false
        }

        return true
    }

    @Throws(IOException::class, InvalidFormatException::class)
    private fun convertProtocolToWorkbook(protocol: Protocol): ByteArrayOutputStream {
        val templateTempFile = File(System.getProperty("user.dir"), "tmp.xlsx")
        try {
            copyFileFromStream(Main::class.java.getResourceAsStream("raw/template_resonance.xlsx"), templateTempFile)
        } catch (e: IOException) {
            Toast.makeText("Ошибка").show(Toast.ToastType.ERROR)
        }

        XSSFWorkbook(templateTempFile).use { wb ->
            val sheet = wb.getSheetAt(0)
            for (i in 0..99) {
                val row = sheet.getRow(i)
                if (row != null) {
                    for (j in 0..19) {
                        val cell = row.getCell(j)
                        if (cell != null && cell.cellTypeEnum == CellType.STRING) {
                            when (cell.stringCellValue) {
                                "\$PROTOCOL_NUMBER$" -> {
                                    val id = protocol.id
                                    if (id != 0L) {
                                        cell.setCellValue(id.toString() + "")
                                    } else {
                                        cell.setCellValue("")
                                    }
                                }
                                "\$OBJECT$" -> {
                                    val objectName = protocol.type
                                    if (objectName != null) {
                                        cell.setCellValue(objectName)
                                    } else {
                                        cell.setCellValue("")
                                    }
                                }
                                "\$SERIAL_NUMBER$" -> {
                                    val serialNumber = protocol.serialNumber
                                    if (serialNumber != null && !serialNumber.isEmpty()) {
                                        cell.setCellValue(serialNumber)
                                    } else {
                                        cell.setCellValue("")
                                    }
                                }
                                "\$TYPEEXPERIMENT$" -> cell.setCellValue(protocol.typeExperiment)
                                "\$POS1$" -> cell.setCellValue(protocol.position1)
                                "\$POS2$" -> cell.setCellValue(protocol.position2)
                                "\$POS1NAME$" -> cell.setCellValue(String.format("/%s/", protocol.position1FullName))
                                "\$POS2NAME$" -> cell.setCellValue(String.format("/%s/", protocol.position2FullName))
                                "\$DATE$" -> {
                                    val sdf = SimpleDateFormat("dd-MM-yy")
                                    cell.setCellValue(sdf.format(protocol.millis))
                                }
                                else -> if (cell.stringCellValue.contains("$")) {
                                    cell.setCellValue("")
                                }

                            }

                        }
                    }
                }

            }
            fillParameters(wb, protocol.points)
            val out = ByteArrayOutputStream()
            try {
                wb.write(out)
            } finally {
                out.close()
            }
            return out
        }
    }

    private fun fillParameters(wb: XSSFWorkbook, points: ArrayList<Point>) {
        val sheet = wb.getSheetAt(0)
        var row: Row
        var cellStyle: XSSFCellStyle = generateStyles(wb) as XSSFCellStyle
        var rowNum = 28
        row = sheet.createRow(rowNum)
        var columnNum = 5
        for (i in points.indices) {
            columnNum = fillOneCell(row, columnNum, cellStyle, points[i].measuringUOut)
            columnNum = fillOneCell(row, columnNum, cellStyle, points[i].measuringIC)
            columnNum = fillOneCell(row, columnNum, cellStyle, points[i].measuringTime)
            row = sheet.createRow(++rowNum)
            columnNum = 5
        }
    }

    private fun fillOneCell(row: Row, columnNum: Int, cellStyle: XSSFCellStyle, time: String): Int {
        val cell: Cell = row.createCell(columnNum)
        cell.cellStyle = cellStyle
        cell.setCellValue(time)
        return columnNum + 1
    }

    private fun generateStyles(wb: XSSFWorkbook): CellStyle {
        val headStyle: CellStyle = wb.createCellStyle()
        headStyle.wrapText = true
        headStyle.borderBottom = BorderStyle.THIN
        headStyle.borderTop = BorderStyle.THIN
        headStyle.borderLeft = BorderStyle.THIN
        headStyle.borderRight = BorderStyle.THIN
        headStyle.alignment = HorizontalAlignment.CENTER
        headStyle.verticalAlignment = VerticalAlignment.CENTER
        return headStyle
    }

}