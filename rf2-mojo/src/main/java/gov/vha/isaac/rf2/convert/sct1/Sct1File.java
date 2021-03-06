package gov.vha.isaac.rf2.convert.sct1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.apache.maven.plugin.MojoFailureException;

public class Sct1File implements Comparable<Object> {

    File file;
    Date revDate;
    long time;
    
    public Sct1File(File f, Date d) {
        this.file = f;
        this.revDate = d;
        this.time = d.getTime();
    }

    public String toString() {
        return file.getPath();
    }

    @Override
    public int compareTo(Object o) {
        Sct1File tmp = (Sct1File) o;
        return revDate.compareTo(tmp.revDate);
    }

    public static int countFileLines(Sct1File rf1) throws IOException {
        int lineCount = 0;
        BufferedReader br = null;

        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(rf1.file), "UTF-8"));
            try {
                while (br.readLine() != null) {
                    lineCount++;
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                throw new IOException("FAILED: error counting lines in " + rf1.file, ex);
            } finally {
                br.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new IOException("FAILED: error open BufferedReader for " + rf1.file, ex);
        }

        // lineCount NOTE: COUNT -1 BECAUSE FIRST LINE SKIPPED
        // lineCount NOTE: REQUIRES THAT LAST LINE IS VALID RECORD
        return lineCount - 1;
    }
    
    private static boolean inDateRange(Date revDate, Date dateStart, Date dateStop)
            throws MojoFailureException {

        if (dateStart != null && revDate.compareTo(dateStart) < 0)
            return false; // precedes start date

        if (dateStop != null && revDate.compareTo(dateStop) > 0)
            return false; // after end date

        return true;
    }

   private static Date getFileRevDate(File f) throws ParseException {
        int pos;
        Date d1 = null;
        Date d2 = null;
        // Check file name for date yyyyMMdd
        // EXAMPLE: ../net/nhs/uktc/ukde/sct1_relationships_uk_drug_20090401.txt
        pos = f.getName().length() - 12; // "yyyyMMdd.txt"
        String s1 = f.getName().substring(pos, pos + 8);
        // normalize date format
        s1 = s1.substring(0, 4) + "-" + s1.substring(4, 6) + "-" + s1.substring(6);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        dateFormat.setLenient(false);
        try {
            d1 = dateFormat.parse(s1);
        } catch (ParseException pe) {
            d1 = null;
        }

        // Check path for date yyyy-MM-dd
        // EXAMPLE: ../org/snomed/2003-01-31
        pos = f.getParent().length() - 10; // "yyyy-MM-dd"
        String s2 = f.getParent().substring(pos);
        try {
            d2 = dateFormat.parse(s2);
        } catch (ParseException pe) {
            d2 = null;
        }

        //
        if ((d1 != null) && (d2 != null)) {
            if (d1.equals(d2)) {
                return d1;
            } else {
                throw new ParseException("FAILED: file name date "
                        + "and directory name date do not agree. ", pos);
            }
        } else if (d1 != null) {
            return d1;
        } else if (d2 != null) {
            return d2;
        } else {
            throw new ParseException("FAILED: date can not be determined"
                    + " from either file name date or directory name date.", pos);
        }
    }

    public static List<Sct1File> getSctFiles(String wDir, String subDir, String rootDir,
            String prefix, String postfix) throws ParseException {

        ArrayList<Sct1File> listOfFiles = new ArrayList<Sct1File>();

        File f1 = new File(new File(wDir, subDir), rootDir);
        ArrayList<File> fv = new ArrayList<File>();
        listFilesRecursive(fv, f1, "sct1_" + prefix, postfix);

        File[] files = new File[0];
        files = fv.toArray(files);
        Arrays.sort(files);

        for (File f2 : files) {
            // ADD Sct1File Entry
            Date revDate = Sct1File.getFileRevDate(f2);
            Sct1File fo = new Sct1File(f2, revDate);
            listOfFiles.add(fo);
        }

        Collections.sort(listOfFiles);

        return listOfFiles;
    }
    
    public static List<Sct1File> getSctFiles(String wDir, String subDir, String rootDir,
            String prefix, String postfix, Date dateStart, Date dateStop) throws ParseException,
            MojoFailureException {

        ArrayList<Sct1File> listOfFiles = new ArrayList<Sct1File>();

        File f1 = new File(new File(wDir, subDir), rootDir);
        ArrayList<File> fv = new ArrayList<File>();
        listFilesRecursive(fv, f1, "sct1_" + prefix, postfix);

        File[] files = new File[0];
        files = fv.toArray(files);
        Arrays.sort(files);

        for (File f2 : files) {
            // ADD Sct1File Entry
            Date revDate = Sct1File.getFileRevDate(f2);
            if (inDateRange(revDate, dateStart, dateStop)) {
                Sct1File fo = new Sct1File(f2, revDate);
                listOfFiles.add(fo);
            }
        }

        Collections.sort(listOfFiles);

        return listOfFiles;
    }


    private static void listFilesRecursive(ArrayList<File> list, File root, String prefix,
            String postfix) {
        if (root.isFile()) {
            list.add(root);
            return;
        }
        File[] files = root.listFiles();
        Arrays.sort(files);
        for (int i = 0; i < files.length; i++) {
            String name = files[i].getName().toUpperCase();

            if (files[i].isFile() && name.endsWith(postfix.toUpperCase())
                    && name.contains(prefix.toUpperCase())) {
                list.add(files[i]);
            }
            if (files[i].isDirectory()) {
                listFilesRecursive(list, files[i], prefix, postfix);
            }
        }
    }

}
