package laam;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class Test extends JFrame {

	public static final long serialVersionUID = 0;

	private JPanel contentPanel;
	private JButton jButtonList;
	private JButton jButtonZip;
	private JButton jButtonUnzip;
	private JButton jButtonDeleteZip;
	private JButton jButtonDeleteNonZip;

	private Directory d;

	private void addElement(Container c, Component e, int x, int y, int w, int h) {
		e.setBounds(x, y, w, h);
		c.add(e);
	}

	private class simpleForm {
		public simpleForm() {
			contentPanel = (JPanel) getContentPane();
			contentPanel.setLayout(null);
			jButtonList = new JButton("List files");
			addElement(contentPanel, jButtonList, 20, 20, 200, 40);
			jButtonZip = new JButton("Zip files");
			addElement(contentPanel, jButtonZip, 20, 80, 200, 40);
			jButtonUnzip = new JButton("Unzip files");
			addElement(contentPanel, jButtonUnzip, 240, 80, 200, 40);
			jButtonDeleteZip = new JButton("Delete Zip files");
			addElement(contentPanel, jButtonDeleteZip, 20, 140, 200, 40);
			jButtonDeleteNonZip = new JButton("Delete Non-Zip files");
			addElement(contentPanel, jButtonDeleteNonZip, 240, 140, 200, 40);

			jButtonList.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					d.list();
				}
			});

			jButtonZip.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					d.zip();
				}
			});

			jButtonUnzip.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					d.unzip();
				}
			});

			jButtonDeleteZip.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					d.deleteZip();
				}
			});

			jButtonDeleteNonZip.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					d.deleteNonZip();
				}
			});

			setTitle("A Simple Form");
			setSize(720, 600);
			setLocation(new Point(100, 100));
			setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
			setVisible(true);
		}
	}

	public Test() {
		d = new Directory();
		new simpleForm();
	}
}
