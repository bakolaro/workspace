package laam;

import java.io.*;
import java.util.zip.*;

public class Directory {
	private final int BUFFER = 2048;

	private final String PATH = "uploaded";

	private void unzip(String path, String filename, int buffer) {
		try {
			BufferedOutputStream dest = null;
			FileInputStream fis = new FileInputStream(path + "/" + filename);
			ZipInputStream zis = new ZipInputStream(
					new BufferedInputStream(fis));
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				System.out.println("\tExtracting: " + entry);
				int count;
				byte data[] = new byte[buffer];
				FileOutputStream fos = new FileOutputStream(path + "/"
						+ entry.getName());
				dest = new BufferedOutputStream(fos, buffer);
				while ((count = zis.read(data, 0, buffer)) != -1) {
					dest.write(data, 0, count);
				}
				dest.flush();
				dest.close();
			}
			zis.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void unzip(String path, String[] filenames, int buffer) {
		if (filenames != null)
			for (int i = 0; i < filenames.length; i++)
				unzip(path, filenames[i], buffer);
	}

	private void unzip(String path, int buffer) {
		File dir = new File(path);
		FilenameFilter filter_Zip = new FilterZip();
		unzip(path, dir.list(filter_Zip), buffer);
	}

	public void unzip() {
		System.out.println("Разархивиране на ZIP файловете в " + PATH);
		unzip(PATH, BUFFER);
	}

	private void zip(String path, String filename, int buffer) {
		try {
			String noExt = filename.substring(0, filename.toLowerCase()
					.lastIndexOf("."));

			BufferedInputStream origin = null;
			FileOutputStream dest = new FileOutputStream(path + "/" + noExt
					+ ".zip");
			ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(
					dest));
			byte data[] = new byte[buffer];
			System.out.println("\tAdding: " + filename);
			FileInputStream fi = new FileInputStream(path + "/" + filename);
			origin = new BufferedInputStream(fi, buffer);
			ZipEntry entry = new ZipEntry(filename); // име на файла в архива
			out.putNextEntry(entry);
			int count;
			while ((count = origin.read(data, 0, buffer)) != -1) {
				out.write(data, 0, count);
			}
			origin.close();
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void zip(String path, String[] filenames, int buffer) {
		if (filenames != null)
			for (int i = 0; i < filenames.length; i++)
				zip(path, filenames[i], buffer);
	}

	private void zip(String path, int buffer) {
		File dir = new File(path);
		FilenameFilter filter_Xml = new FilterXml();
		zip(path, dir.list(filter_Xml), buffer);
	}

	public void zip() {
		System.out.println("Архивиране на файловете в " + PATH);
		zip(PATH, BUFFER);
	}

	private boolean delete(String path, String filename) {
		File f = new File(path + "/" + filename);
		return f.delete();
	}

	private void delete(String path, String[] filenames) {
		if (filenames != null)
			for (int i = 0; i < filenames.length; i++) {
				if (delete(path, filenames[i]))
					System.out.println("\tИзтриване на " + filenames[i]
							+ " (ok)");
				else
					System.out.println("\tИзтриване на " + filenames[i]
							+ " (грешка)");
			}
	}

	private void delete(FilenameFilter filter) {
		File dir = new File(PATH);
		delete(PATH, dir.list(filter));
	}

	public void delete() {
		System.out.println("Изтриване на файловете в " + PATH);
		File dir = new File(PATH);
		delete(PATH, dir.list());
	}

	public void deleteZip() {
		System.out.println("Изтриване на ZIP файловете в " + PATH);
		FilenameFilter filter_Zip = new FilterZip();
		delete(filter_Zip);
	}

	public void deleteNonZip() {
		System.out.println("Изтриване на не-ZIP файловете в " + PATH);
		FilenameFilter filter_NonZip = new FilterNonZip();
		delete(filter_NonZip);
	}

	private void list(String path) {
		File dir = new File(path);
		String[] filenames = dir.list();
		System.out.println("Заредени файлове");
		if (filenames != null) {
			for (int i = 0; i < filenames.length; i++) {
				System.out.println("\t" + filenames[i]);
			}
		}
	}

	public void list() {
		System.out.println("Списък на файловете в " + PATH);
		this.list(PATH);
	}
}
