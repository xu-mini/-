import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelResolveUtils {
    private final String XLSX = ".xlsx";
    private final String XLS=".xls";

    /**
     * 获取Excel文件（.xls和.xlsx都支持）
     * @param file
     * @return  解析excle后的Json数据
     * @throws IOException
     * @throws FileNotFoundException
     * @throws InvalidFormatException
     */
    public List<String> readExcel(File file) throws Exception{
        int res = checkFile(file);

        if (res == 0) {
            System.out.println("File not found");
        }else if (res == 1) {
            return readXLSX(file);
        }else if (res == 2) {
            return readXLS(file);
        }else{
            System.out.println("暂不支持该文件格式");
        }
        return new ArrayList<>();
    }
    /**
     * 判断File文件的类型
     * @param file 传入的文件
     * @return 0-文件为空，1-XLSX文件，2-XLS文件，3-其他文件
     */
    public int checkFile(File file){
        if (file==null) {
            return 0;
        }
        String flieName = file.getName();
        if (flieName.endsWith(XLSX)) {
            return 1;
        }
        if (flieName.endsWith(XLS)) {
            return 2;
        }
        return 3;
    }

    /**
     * 读取XLSX文件
     * @param file
     * @return
     * @throws IOException
     * @throws InvalidFormatException
     */
    public List<String> readXLSX(File file) throws InvalidFormatException, IOException{
        Workbook book = new XSSFWorkbook(file);
        Sheet sheet = book.getSheetAt(0);
        return read(sheet, book);
    }

    /**
     * 读取XLS文件
     * @param file
     * @return
     * @throws IOException
     * @throws FileNotFoundException
     */
    public List<String> readXLS(File file) throws FileNotFoundException, IOException{
        POIFSFileSystem poifsFileSystem = new POIFSFileSystem(new FileInputStream(file));
        Workbook book = new HSSFWorkbook(poifsFileSystem);
        Sheet sheet = book.getSheetAt(0);
        return read(sheet, book);
    }

    /**
     * 解析数据
     * @param sheet 表格sheet对象
     * @param book 用于流关闭
     * @return
     * @throws IOException
     */
    public List<String> read(Sheet sheet,Workbook book) throws IOException{
        int rowStart = sheet.getFirstRowNum();	// 首行下标
        int rowEnd = sheet.getLastRowNum();	// 尾行下标
        // 如果首行与尾行相同，表明只有一行，直接返回空数组
        if (rowStart == rowEnd) {
            book.close();
            return new ArrayList<>();
        }
        // 获取第一行JSON对象键
        Row firstRow = sheet.getRow(rowStart);
        int cellStart = firstRow.getFirstCellNum();
        int cellEnd = firstRow.getLastCellNum();
        Map<Integer, String> keyMap = new HashMap<Integer, String>();
        for (int j = cellStart; j < cellEnd; j++) {
            keyMap.put(j,getValue(firstRow.getCell(j), rowStart, j, book, true));
        }
        // 获取每行JSON对象的值
        List<String> res = new ArrayList<>();

        for(int i = rowStart+1; i <= rowEnd ; i++) {
            Row eachRow = sheet.getRow(i);
            if (eachRow != null) {
                StringBuffer sb = new StringBuffer(keyMap.get(0));
                for (int k = cellStart; k < cellEnd; k++) {
                    if (eachRow != null) {
                        String val = getValue(eachRow.getCell(k), i, k, book, false);
                        sb.append(val);		// 所有数据添加到里面，用于判断该行是否为空
                        if(!val.toString().equals("")){
                            res.add(sb.toString());
                        }
                    }
                }
            }
        }
        book.close();
        return res;
    }

    /**
     * 获取每个单元格的数据
     * @param cell 单元格对象
     * @param rowNum 第几行
     * @param index 该行第几个
     * @param book 主要用于关闭流
     * @param isKey 是否为键：true-是，false-不是。 如果解析Json键，值为空时报错；如果不是Json键，值为空不报错
     * @return
     * @throws IOException
     */
    public String getValue(Cell cell,int rowNum,int index,Workbook book,boolean isKey) throws IOException{

        // 空白或空
        if (cell == null || cell.getCellTypeEnum()== CellType.BLANK ) {
            if (isKey) {
                book.close();
                throw new NullPointerException(String.format("the key on row %s index %s is null ", ++rowNum,++index));
            }else{
                return "";
            }
        }

        // 0. 数字 类型
        if (cell.getCellTypeEnum() == CellType.NUMERIC) {
            if (HSSFDateUtil.isCellDateFormatted(cell)) {
                Date date = cell.getDateCellValue();
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                return df.format(date);
            }
            String val = cell.getNumericCellValue()+"";
            val = val.toUpperCase();
            if (val.contains("E")) {
                val = val.split("E")[0].replace(".", "");
            }
            return val;
        }

        // 1. String类型
        if (cell.getCellTypeEnum() == CellType.STRING) {
            String val = cell.getStringCellValue();
            if (val == null || val.trim().length()==0) {
                if (book != null) {
                    book.close();
                }
                return "";
            }
            return val.trim();
        }

        // 2. 公式 CELL_TYPE_FORMULA
        if (cell.getCellTypeEnum() == CellType.FORMULA) {
            return cell.getStringCellValue();
        }

        // 4. 布尔值 CELL_TYPE_BOOLEAN
        if (cell.getCellTypeEnum() == CellType.BOOLEAN) {
            return cell.getBooleanCellValue()+"";
        }

        // 5.	错误 CELL_TYPE_ERROR
        return "";
    }
}
