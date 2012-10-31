package laam;

import java.io.*;
import java.nio.charset.Charset;
import java.sql.*;
import java.sql.Date;
import java.util.*;

public class Parselot {

	private Connection conn;

	private Statement stmt;

	private final String STYLE = "th,td {border:solid green 2pt;} " + ".x"
			+ java.sql.Types.VARCHAR + " {background:#88dd99;} " + ".x"
			+ java.sql.Types.LONGVARCHAR + " {background:#66bb88;} " + ".x"
			+ java.sql.Types.INTEGER + " {background:#bbcc44;} " + ".x"
			+ java.sql.Types.DECIMAL + " {background:#22bb00;} " + ".x"
			+ java.sql.Types.BIT + " {background:#76aacc;} " + ".x"
			+ java.sql.Types.DATE + " {background:#ee6699;} " + ".x"
			+ java.sql.Types.TIME + " {background:#aa77ff;} ";

	private abstract class Field {
		private String xmlTag, sqlHeading;
		private int sqlType;

		public Field(String xmlTag) {
			this.xmlTag = xmlTag;
			this.sqlHeading = xmlTag;
		}

		public Field(String xmlTag, String suffix) {
			this.xmlTag = xmlTag;
			this.sqlHeading = xmlTag + "__" + suffix;
		}

		public abstract String getDef();

		public abstract void setStatement(PreparedStatement pstmt, int n,
				String s) throws SQLException;

		public void setNull(PreparedStatement pstmt, int n) throws SQLException {
			pstmt.setNull(n, this.sqlType);
		}

		public String getXmlTag() {
			return this.xmlTag;
		}

		public String getSqlHeading() {
			return this.sqlHeading;
		}

		public void setType(int t) {
			this.sqlType = t;
		}
	}

	private class VarChar_ extends Field {
		private int width;

		public VarChar_(String xmlTag, int width) {
			super(xmlTag);
			setType(java.sql.Types.VARCHAR);
			this.width = width;
		}

		public VarChar_(String xmlTag, String suffix, int width) {
			super(xmlTag, suffix);
			setType(java.sql.Types.VARCHAR);
			this.width = width;
		}

		@Override
		public String getDef() {
			return getSqlHeading() + " varchar(" + this.width + "), ";
		}

		@Override
		public void setStatement(PreparedStatement p, int n, String s)
				throws SQLException {
			p.setString(n, s);
		}
	}

	private class Text_ extends Field {
		private int width;

		public Text_(String xmlTag, int width) {
			super(xmlTag);
			setType(java.sql.Types.LONGVARCHAR);
			this.width = width;
		}

		public Text_(String xmlTag, String suffix, int width) {
			super(xmlTag, suffix);
			setType(java.sql.Types.LONGVARCHAR);
			this.width = width;
		}

		@Override
		public String getDef() {
			return getSqlHeading() + " text(" + this.width + "), ";
		}

		@Override
		public void setStatement(PreparedStatement p, int n, String s)
				throws SQLException {
			p.setString(n, s);
		}
	}

	private class Int_ extends Field {
		private int width;

		public Int_(String xmlTag, int width) {
			super(xmlTag);
			setType(java.sql.Types.INTEGER);
			this.width = width;
		}

		public Int_(String xmlTag, String suffix, int width) {
			super(xmlTag, suffix);
			setType(java.sql.Types.INTEGER);
			this.width = width;
		}

		@Override
		public String getDef() {
			return getSqlHeading() + " int(" + this.width + "), ";
		}

		@Override
		public void setStatement(PreparedStatement p, int n, String s)
				throws SQLException {
			p.setInt(n, Integer.parseInt(s));
		}
	}

	private class Decimal_ extends Field {
		private int width, precision;

		public Decimal_(String xmlTag, int width, int precision) {
			super(xmlTag);
			setType(java.sql.Types.DECIMAL);
			this.width = width;
			this.precision = precision;
		}

		@Override
		public String getDef() {
			return getSqlHeading() + " decimal(" + this.width + ","
					+ this.precision + "), ";
		}

		@Override
		public void setStatement(PreparedStatement p, int n, String s)
				throws SQLException {
			p.setDouble(n, Double.parseDouble(s));
		}
	}

	private class Boolean_ extends Field {
		public Boolean_(String xmlTag) {
			super(xmlTag);
			setType(java.sql.Types.BIT);
		}

		public Boolean_(String xmlTag, String suffix) {
			super(xmlTag, suffix);
			setType(java.sql.Types.BIT);
		}

		@Override
		public String getDef() {
			return getSqlHeading() + " boolean, ";
		}

		@Override
		public void setStatement(PreparedStatement p, int n, String s)
				throws SQLException {
			boolean b = false;
			if (s != null
					&& (s.equalsIgnoreCase("true") || s.equals("1")
							|| s.equalsIgnoreCase("yes") || s
								.equalsIgnoreCase("y")))
				b = true;
			p.setBoolean(n, b);
		}
	}

	private class Date_ extends Field {
		public Date_(String xmlTag) {
			super(xmlTag);
			setType(java.sql.Types.DATE);
		}

		public Date_(String xmlTag, String suffix) {
			super(xmlTag, suffix);
			setType(java.sql.Types.DATE);
		}

		@Override
		public String getDef() {
			return getSqlHeading() + " date, ";
		}

		@Override
		public void setStatement(PreparedStatement p, int n, String s)
				throws SQLException {
			p.setDate(n, Date.valueOf(s));
		}
	}

	private class Time_ extends Field {
		public Time_(String xmlTag) {
			super(xmlTag);
			setType(java.sql.Types.TIME);
		}

		@Override
		public String getDef() {
			return getSqlHeading() + " time, ";
		}

		@Override
		public void setStatement(PreparedStatement p, int n, String s)
				throws SQLException {
			p.setTime(n, Time.valueOf(s));
		}
	}

	private class Row {
		public String xmlTag, sqlNew, sqlIns, sqlKeyQry;

		public Field[] fields;
		public int[] keyFields;

		private Row(String sqlTableName, String xmlTag, Field[] fields,
				int[] keyFields) {
			this.xmlTag = xmlTag;

			String sqlFieldsDef = "id int not null auto_increment, ";
			for (int i = 0; i < fields.length; i++)
				sqlFieldsDef += fields[i].getDef();
			sqlFieldsDef += "primary key (id)";

			String sqlUniq = new String();
			if (keyFields.length > 0) {
				String sqlKeyFieldsList = new String();
				for (int i = 0; i < keyFields.length - 1; i++) {
					sqlKeyFieldsList += fields[keyFields[i]].getSqlHeading()
							+ ", ";
					sqlUniq += fields[keyFields[i]].getSqlHeading() + "=? and ";
				}
				sqlKeyFieldsList += fields[keyFields[keyFields.length - 1]].sqlHeading;
				sqlUniq += fields[keyFields[keyFields.length - 1]].sqlHeading
						+ "=?";

				sqlFieldsDef += ", unique key(" + sqlKeyFieldsList + ")";
			} else {
				sqlUniq = "false";
			}

			this.sqlNew = "create table if not exists " + sqlTableName + " ("
					+ sqlFieldsDef + ")";

			String sqlFieldsList = new String(), sqlPlaceholders = new String();
			for (int i = 0; i < fields.length - 1; i++) {
				sqlFieldsList += fields[i].getSqlHeading() + ", ";
				sqlPlaceholders += "?, ";
			}
			sqlFieldsList += fields[fields.length - 1].sqlHeading;
			sqlPlaceholders += "?";

			this.sqlIns = "insert into " + sqlTableName + " (" + sqlFieldsList
					+ ") values(" + sqlPlaceholders + ")";
			this.sqlKeyQry = "select * from " + sqlTableName + " where "
					+ sqlUniq;
			this.fields = fields;
			this.keyFields = keyFields;
		}
	}

	private final Row[] TABS = new Row[] {
			new Row("ambReg", "AmbulRegister", new Field[] {
					new VarChar_("ActnType", 5), new VarChar_("ActnCode", 6),
					new VarChar_("UIN", 10), new VarChar_("SIMPCode", 2),
					new Int_("Cnt", 10), new Int_("AmbId", 10), }, new int[] {}),
			new Row("labPrices", "LAB_ACTIVITIES", new Field[] {
					new VarChar_("CODE", 5), new VarChar_("NAME", 250),
					new Decimal_("PRICE", 20, 2),
					new VarChar_("DOCTOR_SPEC", 2), new VarChar_("OK", 50) },
					new int[] { 0, 3 }),
			new Row("ambPrices", "AmbulActivities", new Field[] {
					new VarChar_("Type", 7), new VarChar_("Code", 6),
					new VarChar_("Name", 250), new Decimal_("Price", 20, 2),
					new VarChar_("SIMPCode", 2), new VarChar_("OK", 50) },
					new int[] { 1, 4 }),
			new Row("labClaims", "LABS_CLAIM", new Field[] {
					new VarChar_("RHIF", 2),
					new VarChar_("LABS_HEREGISTRATIONNUMBER", 10),
					new VarChar_("LABS_NAME", 250),
					new VarChar_("CONTRACT_NUMBER", 20),
					new Decimal_("OTHER_PAYMENT", 20, 2),
					new Decimal_("INVOICE_NUMBER", 10, 0),
					new Date_("INVOICE_DATE"),
					new Decimal_("INVOICE_ACCOUNT", 20, 2),
					new Date_("CLAIM_DATE_FROM"), new Date_("CLAIM_DATE_TO"),
					new VarChar_("OK", 50) }, new int[] { 1, 6 }),
			new Row("labForms", "LABS_FORMS", new Field[] {
					new Int_("SHEET_NUMBER", 6),
					new Int_("AMB_SHEET_NUMBER", 6),
					new VarChar_("RHIF", "PATIENT", 2),
					new VarChar_("HEALTH_REGION", 2),
					new VarChar_("GRAONO", 10), new Int_("NATIONALITY", 1),
					new Date_("DATE_OF_BIRTH"), new VarChar_("FIRSTNAME", 30),
					new VarChar_("SECONDNAME", 30),
					new VarChar_("FAMILYNAME", 50),
					new VarChar_("HEREGISTRATION_NUMBER", 10),
					new Int_("DOCTOR_TYPE", 1), new VarChar_("DOCTOR_UIN", 10),
					new VarChar_("DOCTOR_DEPUTY_UIN", 10),
					new VarChar_("DOCTOR_NAME", 100),
					new VarChar_("DOCTOR_SPEC", 2), new Int_("TYPE", 2),
					new VarChar_("ICD", 6), new VarChar_("ICD_Linked", 6),
					new Date_("CREATION_DATE"),
					new VarChar_("DOCTOR_UIN", "EXE", 10),
					new VarChar_("DOCTOR_NAME", "EXE", 100),
					new VarChar_("DOCTOR_SPEC", "EXE", 2),
					new Date_("EXECUTION_DATE"), new Int_("LABS_CLAIM", 10),
					new VarChar_("OK", 50) }, new int[] {}),
			new Row("labProcs", "PROCS", new Field[] {
					new VarChar_("PROCEDURE", "X", 5), new Boolean_("EXECUTE"),
					new Int_("LABS_FORMS", 10), new VarChar_("OK", 50) },
					new int[] {}),
			new Row("ambPractices", "Practice", new Field[] {
					new VarChar_("PracticeCode", 10),
					new VarChar_("PracticeName", 150), new Date_("DateFrom"),
					new Date_("DateTo"), new Boolean_("ContrHA"),
					new VarChar_("OK", 50) }, new int[] { 0, 2 }),
			new Row("ambDoctors", "Doctor", new Field[] {
					new VarChar_("UIN", 10), new VarChar_("EGN", 10),
					new VarChar_("FullName", 100), new VarChar_("SIMPCode", 2),
					new Int_("Practice", 10), new VarChar_("OK", 50) },
					new int[] { 0, 3, 4 }),
			new Row("ambLists", "AmbList", new Field[] {
					new VarChar_("NoAl", 6),
					new Date_("dataAl"),
					new Time_("time"),
					new Boolean_("Pay"),
					new VarChar_("Type", 7),
					new Decimal_("Value", 20, 2),
					new Boolean_("H_S"),
					new VarChar_("UINH_S", 10),
					new VarChar_("FullNameH_S", 100),
					new VarChar_("EGN", "Patient", 10),
					new Date_("dateBirth"),
					new Boolean_("Sex"),
					new VarChar_("SS_No", 20),
					new VarChar_("COUNTRYCODE", 2),
					new VarChar_("COUNTRYIDNO", 20),
					new VarChar_("Type_CERTIFICATE", 6),
					new Date_("DateIssue"),
					new Date_("DateFrom", "Patient"),
					new Date_("DateTo", "Patient"),
					new VarChar_("EHIC_No", 20),
					new VarChar_("PersID_No", 20),
					new VarChar_("RZOK", 2),
					new VarChar_("ZdrRajon", 2),
					new VarChar_("Given", 30),
					new VarChar_("Sur", 30),
					new VarChar_("Family", 30),
					new VarChar_("Address", 100),
					new Boolean_("Disadv"),
					new Boolean_("Incidentally"),

					new VarChar_("imeMD", 250),
					new VarChar_("MKB", 6),
					new VarChar_("imeLinkD", 250),
					new VarChar_("MKBLinkD", 6),

					new VarChar_("imeD", "1", 250),
					new VarChar_("MKB", "1", 6),
					new VarChar_("imeLinkD", "1", 250),
					new VarChar_("MKBLinkD", "1", 6),

					new VarChar_("imeD", "2", 250),
					new VarChar_("MKB", "2", 6),
					new VarChar_("imeLinkD", "2", 250),
					new VarChar_("MKBLinkD", "2", 6),

					new VarChar_("imeD", "3", 250),
					new VarChar_("MKB", "3", 6),
					new VarChar_("imeLinkD", "3", 250),
					new VarChar_("MKBLinkD", "3", 6),

					new VarChar_("imeD", "4", 250),
					new VarChar_("MKB", "4", 6),
					new VarChar_("imeLinkD", "4", 250),
					new VarChar_("MKBLinkD", "4", 6),
					// UBound=4
					new Text_("Anamnesa", 4000),
					new Text_("HState", 4000),
					new Text_("Examine", 4000),
					new Text_("Nonreimburce", 4000),
					// unbounded::unbounded
					new Int_("VidNapr", 1), new VarChar_("NoNapr", 6),
					new Int_("NaprFor", 2), new Date_("dateNapr"),

					new VarChar_("NoAmbL", 6), new Date_("dateAmbL"),
					new VarChar_("PracticeCode", "SendedFrom", 10),
					new VarChar_("UIN", "SendedFrom", 10),
					new VarChar_("SIMPCode", "SendedFrom", 2),
					new Boolean_("H_S", "SendedFrom"),
					new VarChar_("UINH_S", "SendedFrom", 10),

					new Boolean_("Consult"), new Boolean_("Child"),
					new Boolean_("Mother"), new Int_("GestWeek", 2),
					new Boolean_("Over18"), new Boolean_("Risk"),
					new Boolean_("Disp"), new Boolean_("Hosp"),
					new Boolean_("RpHosp"), new Boolean_("LKKVisit"),
					new Int_("CountLKKMembers", 1),
					new VarChar_("UIN", "LKKMember_1", 10),
					new VarChar_("SIMPCode", "LKKMember_1", 2),
					new VarChar_("UIN", "LKKMember_2", 10),
					new VarChar_("SIMPCode", "LKKMember_2", 2),
					new Boolean_("Telk"), new VarChar_("ExamType", 2),
					new VarChar_("CodeImun", "1", 2),
					new VarChar_("CodeImun", "2", 2),
					new VarChar_("CodeImun", "3", 2), new VarChar_("Mantu", 2),

					new VarChar_("kodSp", "1", 2), new Int_("SpFor", "1", 2),
					new VarChar_("NoN", "1", 6),
					new VarChar_("SIMPMKB", "11", 13),
					new VarChar_("SIMPMKB", "12", 13),
					new VarChar_("SIMPMKB", "13", 13),

					new VarChar_("kodSp", "2", 2), new Int_("SpFor", "2", 2),
					new VarChar_("NoN", "2", 6),
					new VarChar_("SIMPMKB", "21", 13),
					new VarChar_("SIMPMKB", "22", 13),
					new VarChar_("SIMPMKB", "23", 13),

					new VarChar_("kodSp", "3", 2),
					new Int_("SpFor", "3", 2),
					new VarChar_("NoN", "3", 6),
					new VarChar_("SIMPMKB", "31", 13),
					new VarChar_("SIMPMKB", "32", 13),
					new VarChar_("SIMPMKB", "33", 13),

					new VarChar_("kodSp", "4", 2),
					new Int_("SpFor", "4", 2),
					new VarChar_("NoN", "4", 6),
					new VarChar_("SIMPMKB", "41", 13),
					new VarChar_("SIMPMKB", "42", 13),
					new VarChar_("SIMPMKB", "43", 13),

					new VarChar_("kodSpVSD", "1", 2),
					new Int_("VSDFor", "1", 1),
					new VarChar_("NoNVSD", "1", 6),
					new VarChar_("kodVSD", "1", 6),
					new Boolean_("HospVSD", "1"),
					new VarChar_("VSDMKB", "11", 13),
					new VarChar_("VSDMKB", "12", 13),
					new VarChar_("VSDMKB", "13", 13),

					new VarChar_("kodSpVSD", "2", 2),
					new Int_("VSDFor", "2", 1),
					new VarChar_("NoNVSD", "2", 6),
					new VarChar_("kodVSD", "2", 6),
					new Boolean_("HospVSD", "2"),
					new VarChar_("VSDMKB", "21", 13),
					new VarChar_("VSDMKB", "22", 13),
					new VarChar_("VSDMKB", "23", 13),
					// unbounded::unbounded
					new Boolean_("TalonTELK"),

					new VarChar_("NoLKK", 6), new VarChar_("CodeSpec", "1", 2),
					new VarChar_("CodeSpec", "2", 2),
					new VarChar_("CodeSpec", "3", 2),
					new VarChar_("CodeSpec", "4", 2),
					new VarChar_("CodeSpec", "5", 2),
					new Int_("TypeLKK", 2),

					new VarChar_("HospMKB", "1", 13),
					new VarChar_("HospMKB", "2", 13),
					new VarChar_("PathNum", 5),
					new Date_("DateNapr", "HospNapr"),
					// unbounded::unbounded
					new VarChar_("RpBook", 7),
					// unbounded::unbounded
					new Boolean_("Izvestie"), new Boolean_("EtEpikriza"),
					new Boolean_("Sign"), new Int_("Doctor", 10),
					new VarChar_("OK", 50) }, new int[] {}),
			new Row("ambProcedures", "Procedure", new Field[] {
					new VarChar_("imeP", 250), new VarChar_("kodP", 6),
					new Int_("CountP", 2), new Int_("AmbList", 10),
					new VarChar_("OK", 50) }, new int[] {}), // UBound=4
			new Row("ambReimbursibles", "Reimbursible", new Field[] {
					new VarChar_("DrugCode", 5), new VarChar_("DrugMKB", 6),
					new Int_("Quantity", 6), new Int_("Day", 6),
					new Int_("AmbList", 10), new VarChar_("OK", 50) },
					new int[] {}),
			new Row("ambMDDs", "MDD", new Field[] { new VarChar_("NoMDD", 6),
					new Int_("MDDFor", 2), new VarChar_("kodMDD", "1", 6),
					new VarChar_("kodMDD", "2", 6),
					new VarChar_("kodMDD", "3", 6),
					new VarChar_("kodMDD", "4", 6),
					new VarChar_("kodMDD", "5", 6),
					new VarChar_("kodMDD", "6", 6),
					new VarChar_("MDDMKB", "1", 13), new Int_("AmbList", 10),
					new VarChar_("OK", 50) }, new int[] {}),
			new Row("ambHLists", "HList", new Field[] { new Int_("NoBl", 7),
					new Int_("days", "HList", 3), new VarChar_("HLMKB", 6),
					new Date_("HLFromDate"), new Date_("HLToDate"),
					new Boolean_("HLtype"), new Int_("AmbList", 10),
					new VarChar_("OK", 50) }, new int[] {}),
			new Row("ambRpDrugs", "RpDrug", new Field[] {
					new Int_("prescNum", 5),
					new VarChar_("DrugCode", "RpDrug", 5),
					new VarChar_("DrugMKB", "RpDrug", 6),
					new Int_("Quantity", "RpDrug", 6),
					new Int_("Day", "RpDrug", 3), new Int_("AmbList", 10),
					new VarChar_("OK", 50) }, new int[] {}), };

	private static class Qry {
		public String sqlSelect, sqlTitle;

		public Qry(String sqlSelect, String sqlTitle) {
			this.sqlSelect = sqlSelect;
			this.sqlTitle = sqlTitle;
		}
	}

	private static final Qry[] INS = {
			new Qry(
					"insert into ambReg (ActnType, ActnCode, UIN, SIMPCode, Cnt, AmbId) "
							+ "select '1', '1.1', ambDoctors.UIN, ambDoctors.SIMPCode, '1', ambLists.id "
							+ "from ambPractices "
							+ "inner join ambDoctors on ambPractices.id=ambDoctors.Practice "
							+ "inner join ambLists on ambDoctors.id=ambLists.Doctor "
							+ "left join "
							+ "(select ambProcedures.AmbList, ambPrices.SIMPCode, ambPrices.Type "
							+ "from ambProcedures inner join ambPrices on ambProcedures.kodP=ambPrices.Code) x "
							+ "on ambLists.id=x.AmbList and ambDoctors.SIMPCode=x.SIMPCode and x.Type='Ratio' "
							+ "where ambLists.ExamType like '_1' "
							+ "and ambLists.Pay and ambDoctors.SIMPCode<>'23' and ambLists.Consult "
							+ "and not (ambLists.NaprFor=1 and ambLists.SIMPCode__SendedFrom='00' "
							+ "and ambDoctors.SIMPCode in ('17','33','34','35','36','38','39','51','52','53','54')) "
							+ "and x.AmbList is null", "1.1"),
			new Qry(
					"insert into ambReg (ActnType, ActnCode, UIN, SIMPCode, Cnt, AmbId) "
							+ "select '1', '1.2', ambDoctors.UIN, ambDoctors.SIMPCode, '1', ambLists.id "
							+ "from ambPractices inner join ambDoctors inner join ambLists "
							+ "on ambPractices.id=ambDoctors.Practice and ambDoctors.id=ambLists.Doctor "
							+ "where ExamType like '_1' "
							+ "and Pay and Consult "
							+ "and NaprFor=1 and SIMPCode__SendedFrom='00' "
							+ "and SIMPCode in ('17','33','34','35','36','38','39','51','52','53','54')",
					"1.2"),
			new Qry(
					"insert into ambReg (ActnType, ActnCode, UIN, SIMPCode, Cnt, AmbId) "
							+ "select distinct '1', '1.3', ambDoctors.UIN, ambDoctors.SIMPCode, '1', ambLists.id "
							+ "from ambPractices "
							+ "inner join ambDoctors on ambPractices.id=ambDoctors.Practice "
							+ "inner join ambLists on ambDoctors.id=ambLists.Doctor "
							+ "inner join ambProcedures on ambLists.id=ambProcedures.AmbList "
							+ "inner join ambPrices on ambProcedures.kodP=ambPrices.Code "
							+ "and ambDoctors.SIMPCode=ambPrices.SIMPCode and ambPrices.Type='Ratio' "
							+ "where ExamType like '_1' and Pay and Consult",
					"1.3"),
			new Qry(
					"insert into ambReg (ActnType, ActnCode, UIN, SIMPCode, Cnt, AmbId) "
							+ "select '1', '1.4', ambDoctors.UIN, ambDoctors.SIMPCode, '1', ambLists.id "
							+ "from ambPractices inner join ambDoctors inner join ambLists "
							+ "on ambPractices.id=ambDoctors.Practice and ambDoctors.id=ambLists.Doctor "
							+ "where ExamType like '_3' "
							+ "and Pay and Mother and NaprFor<11", "1.4"),
			new Qry(
					"insert into ambReg (ActnType, ActnCode, UIN, SIMPCode, Cnt, AmbId) "
							+ "select '1', '1.5', ambDoctors.UIN, ambDoctors.SIMPCode, '1', ambLists.id "
							+ "from ambPractices inner join ambDoctors inner join ambLists "
							+ "on ambPractices.id=ambDoctors.Practice and ambDoctors.id=ambLists.Doctor "
							+ "where not (ExamType like '_2' or NaprFor is null) "
							+ "and Pay and Disp", "1.5"),
			new Qry(
					"insert into ambReg (ActnType, ActnCode, UIN, SIMPCode, Cnt, AmbId) "
							+ "select '2', '2.1', ambDoctors.UIN, ambDoctors.SIMPCode, '1', ambLists.id "
							+ "from ambPractices "
							+ "inner join ambDoctors on ambPractices.id=ambDoctors.Practice "
							+ "inner join ambLists on ambDoctors.id=ambLists.Doctor "
							+ "left join "
							+ "(select ambProcedures.AmbList, ambPrices.SIMPCode, ambPrices.Type "
							+ "from ambProcedures inner join ambPrices on ambProcedures.kodP=ambPrices.Code) x "
							+ "on ambLists.id=x.AmbList and ambDoctors.SIMPCode=x.SIMPCode and x.Type='Ratio' "
							+ "where ambLists.ExamType like '_2' "
							+ "and ambLists.Pay and ambDoctors.SIMPCode<>'23' and ambLists.Consult "
							+ "and ambDoctors.SIMPCode not in ('17','33','34','35','36','38','39','51','52','53','54') "
							+ "and x.AmbList is null ", "2.1"),
			new Qry(
					"insert into ambReg (ActnType, ActnCode, UIN, SIMPCode, Cnt, AmbId) "
							+ "select '2', '2.2', ambDoctors.UIN, ambDoctors.SIMPCode, '1', ambLists.id "
							+ "from ambPractices "
							+ "inner join ambDoctors on ambPractices.id=ambDoctors.Practice "
							+ "inner join ambLists on ambDoctors.id=ambLists.Doctor "
							+ "left join "
							+ "(select ambProcedures.AmbList, ambPrices.SIMPCode, ambPrices.Type "
							+ "from ambProcedures inner join ambPrices on ambProcedures.kodP=ambPrices.Code) x "
							+ "on ambLists.id=x.AmbList and ambDoctors.SIMPCode=x.SIMPCode and x.Type='Ratio' "
							+ "where ambLists.ExamType like '_2' "
							+ "and ambLists.Pay and ambDoctors.SIMPCode<>'23' and ambLists.Consult "
							+ "and ambDoctors.SIMPCode in ('17','33','34','35','36','38','39','51','52','53','54') "
							+ "and x.AmbList is null", "2.2"),
			new Qry(
					"insert into ambReg (ActnType, ActnCode, UIN, SIMPCode, Cnt, AmbId) "
							+ "select distinct '2', '2.3', ambDoctors.UIN, ambDoctors.SIMPCode, '1', ambLists.id "
							+ "from ambPractices "
							+ "inner join ambDoctors on ambPractices.id=ambDoctors.Practice "
							+ "inner join ambLists on ambDoctors.id=ambLists.Doctor "
							+ "left join "
							+ "(select ambProcedures.AmbList, ambPrices.SIMPCode, ambPrices.Type "
							+ "from ambProcedures inner join ambPrices on ambProcedures.kodP=ambPrices.Code) x "
							+ "on ambLists.id=x.AmbList and ambDoctors.SIMPCode=x.SIMPCode and x.Type='Ratio' "
							+ "where (ambLists.ExamType like '_2' or ambLists.NaprFor is null) "
							+ "and ambLists.Pay and ambDoctors.SIMPCode<>'23' and ambLists.Consult "
							+ "and x.AmbList is not null", "2.3"),
			new Qry(
					"insert into ambReg (ActnType, ActnCode, UIN, SIMPCode, Cnt, AmbId) "
							+ "select '2', '2.4', ambDoctors.UIN, ambDoctors.SIMPCode, '1', ambLists.id "
							+ "from ambPractices inner join ambDoctors inner join ambLists "
							+ "on ambPractices.id=ambDoctors.Practice and ambDoctors.id=ambLists.Doctor "
							+ "where Pay and Mother "
							+ "and (ExamType like '_2' or NaprFor is null or ExamType like '_3' and NaprFor=11)",
					"2.4"),
			new Qry(
					"insert into ambReg (ActnType, ActnCode, UIN, SIMPCode, Cnt, AmbId) "
							+ "select '2', '2.5', ambDoctors.UIN, ambDoctors.SIMPCode, '1', ambLists.id "
							+ "from ambPractices inner join ambDoctors inner join ambLists "
							+ "on ambPractices.id=ambDoctors.Practice and ambDoctors.id=ambLists.Doctor "
							+ "where (ExamType like '_2' or NaprFor is null) "
							+ "and Pay and Disp", "2.5"),
			new Qry(
					"insert into ambReg (ActnType, ActnCode, UIN, SIMPCode, Cnt, AmbId) "
							+ "select '3', '3', ambDoctors.UIN, ambDoctors.SIMPCode, '1', ambLists.id "
							+ "from ambPractices inner join ambDoctors inner join ambLists "
							+ "on ambPractices.id=ambDoctors.Practice and ambDoctors.id=ambLists.Doctor "
							+ "where Pay and Child", "3"),
			new Qry(
					"insert into ambReg (ActnType, ActnCode, UIN, SIMPCode, Cnt, AmbId) "
							+ "select '4', '4', ambDoctors.UIN, ambDoctors.SIMPCode, '1', ambLists.id "
							+ "from ambPractices inner join ambDoctors inner join ambLists "
							+ "on ambPractices.id=ambDoctors.Practice and ambDoctors.id=ambLists.Doctor "
							+ "where Pay and Risk", "4"),
			new Qry(
					"insert into ambReg (ActnType, ActnCode, UIN, SIMPCode, Cnt, AmbId) "
							+ "select '6', '6.1', ambDoctors.UIN, ambDoctors.SIMPCode, '1', ambLists.id "
							+ "from ambPractices inner join ambDoctors inner join ambLists "
							+ "on ambPractices.id=ambDoctors.Practice and ambDoctors.id=ambLists.Doctor "
							+ "where Pay and LKKVisit and CountLKKMembers is not null",
					"6.1"),
			new Qry(
					"insert into ambReg (ActnType, ActnCode, UIN, SIMPCode, Cnt, AmbId) "
							+ "select '6', '6.2', ambLists.UIN__LKKMember_1, ambLists.SIMPCode__LKKMember_1, '1', ambLists.id "
							+ "from ambPractices inner join ambDoctors inner join ambLists "
							+ "on ambPractices.id=ambDoctors.Practice and ambDoctors.id=ambLists.Doctor "
							+ "where Pay and LKKVisit and CountLKKMembers is not null "
							+ "union all "
							+ "select '6', '6.2', ambLists.UIN__LKKMember_2, ambLists.SIMPCode__LKKMember_2, '1', ambLists.id "
							+ "from ambPractices inner join ambDoctors inner join ambLists "
							+ "on ambPractices.id=ambDoctors.Practice and ambDoctors.id=ambLists.Doctor "
							+ "where Pay and LKKVisit and CountLKKMembers=2",
					"6.2"),
			new Qry(
					"insert into ambReg (ActnType, ActnCode, UIN, SIMPCode, Cnt, AmbId) "
							+ "select '6', '6.3', ambDoctors.UIN, ambDoctors.SIMPCode, '1', ambLists.id "
							+ "from ambPractices inner join ambDoctors inner join ambLists "
							+ "on ambPractices.id=ambDoctors.Practice and ambDoctors.id=ambLists.Doctor "
							+ "where Pay and (LKKVisit or Telk) and CountLKKMembers is null",
					"6.3"),
			new Qry(
					"insert into ambReg (ActnType, ActnCode, UIN, SIMPCode, Cnt, AmbId) "
							+ "select '7', KodP, ambDoctors.UIN, ambDoctors.SIMPCode, '1', ambLists.id "
							+ "from ambPractices inner join ambDoctors inner join ambLists "
							+ "inner join ambProcedures inner join ambPrices "
							+ "on ambPractices.id=ambDoctors.Practice and ambDoctors.id=ambLists.Doctor "
							+ "and ambLists.id=ambProcedures.AmbList "
							+ "and ambProcedures.kodP=ambPrices.Code "
							+ "and ambDoctors.SIMPCode=ambPrices.SIMPCode "
							+ "where Pay and not Hosp", "7"),
			new Qry(
					"insert into ambReg (ActnType, ActnCode, UIN, SIMPCode, Cnt, AmbId) "
							+ "select '8', case concat(min(ambPrices.Type),max(ambPrices.Type)) "
							+ "when 'FFFF' then '8.2.1' "
							+ "when 'KTKT' then '8.2.2' else '8.2.3' end "
							+ "as ActnCode, ambDoctors.UIN, ambDoctors.SIMPCode, sum(CountP) as Cnt, ambLists.id "
							+ "from ambPractices inner join ambDoctors inner join ambLists "
							+ "inner join ambProcedures inner join ambPrices "
							+ "on ambPractices.id=ambDoctors.Practice and ambDoctors.id=ambLists.Doctor "
							+ "and ambLists.id=ambProcedures.AmbList "
							+ "and ambProcedures.kodP=ambPrices.Code "
							+ "and ambDoctors.SIMPCode=ambPrices.SIMPCode "
							+ "where Pay and ambDoctors.SIMPCode='23' and Hosp is null and ExamType like '_1' "
							+ "group by PracticeCode, DateFrom, DateTo, UIN, "
							+ "ambDoctors.SIMPCode, ambLists.id", "8"),
			new Qry(
					"insert into ambReg (ActnType, ActnCode, UIN, SIMPCode, Cnt, AmbId) "
							+ "select '8', '8.3', ambDoctors.UIN, ambDoctors.SIMPCode, '1', ambLists.id "
							+ "from ambPractices inner join ambDoctors inner join ambLists "
							+ "on ambPractices.id=ambDoctors.Practice and ambDoctors.id=ambLists.Doctor "
							+ "where Pay and SIMPCode='23' and Hosp is null and ExamType like '_2'",
					"8.3") },
			QRYS = {
					new Qry("show databases", "Бази от данни"),
					new Qry(
							"select LABS_HEREGISTRATIONNUMBER, LABS_NAME, INVOICE_NUMBER, INVOICE_DATE, "
									+ "INVOICE_ACCOUNT, CLAIM_DATE_FROM, CLAIM_DATE_TO, sum(PRICE) as TOTAL "
									+ "from labClaims inner join labForms inner join labProcs inner join labPrices "
									+ "on labClaims.id=labForms.LABS_CLAIM "
									+ "and labForms.id=labProcs.LABS_FORMS "
									+ "and labForms.DOCTOR_SPEC__EXE=labPrices.DOCTOR_SPEC "
									+ "and labProcs.PROCEDURE__X=labPrices.CODE "
									+ "group by LABS_HEREGISTRATIONNUMBER, LABS_NAME, INVOICE_NUMBER, INVOICE_DATE, "
									+ "INVOICE_ACCOUNT, CLAIM_DATE_FROM, CLAIM_DATE_TO",
							"МДД - стойност по месеци и ЛЗ"),
					new Qry(
							"select PracticeCode, DateFrom, DateTo, sum(Value) as Total "
									+ "from ("
									+ "select PracticeCode, DateFrom, DateTo, "
									+ "ActnType, ActnCode, sum(Cnt) as Cnt, sum(Cnt)*Price as Value "
									+ "from ambReg inner join ambLists on ambReg.AmbId=ambLists.id "
									+ "inner join ambDoctors on ambLists.Doctor=ambDoctors.id "
									+ "inner join ambPractices on ambDoctors.Practice=ambPractices.id "
									+ "inner join ambPrices on ambReg.ActnCode=ambPrices.Code "
									+ "and (ambReg.SIMPCode=ambPrices.SIMPCode or ambPrices.SIMPCode='88') "
									+ "group by ActnType, ActnCode, PracticeCode, DateFrom, DateTo"
									+ ") x "
									+ "group by PracticeCode, DateFrom, DateTo "
									+ "order by DateFrom, DateTo, PracticeCode",
							"Амбул. дейност - стойност по месеци и ЛЗ"),
					new Qry(
							"select PracticeCode, DateFrom, DateTo, ambReg.UIN, ambReg.SIMPCode, "
									+ "ActnType, ActnCode, sum(Cnt) as Cnt, sum(Cnt)*Price as Value "
									+ "from ambReg inner join ambLists on ambReg.AmbId=ambLists.id "
									+ "inner join ambDoctors on ambLists.Doctor=ambDoctors.id "
									+ "inner join ambPractices on ambDoctors.Practice=ambPractices.id "
									+ "inner join ambPrices on ambReg.ActnCode=ambPrices.Code "
									+ "and (ambReg.SIMPCode=ambPrices.SIMPCode or ambPrices.SIMPCode='88') "
									+ "group by ActnType, ActnCode, PracticeCode, DateFrom, DateTo, "
									+ "ambReg.UIN, ambReg.SIMPCode "
									+ "order by DateFrom, DateTo, ActnType, ActnCode, PracticeCode, ambReg.SIMPCode, ambReg.UIN",
							"Амбул. дейност - отчети на лекари"),
					new Qry(
							"select PracticeCode, DateFrom, DateTo, "
									+ "ActnType, ActnCode, sum(Cnt) as Cnt, sum(Cnt)*Price as Value "
									+ "from ambReg inner join ambLists on ambReg.AmbId=ambLists.id "
									+ "inner join ambDoctors on ambLists.Doctor=ambDoctors.id "
									+ "inner join ambPractices on ambDoctors.Practice=ambPractices.id "
									+ "inner join ambPrices on ambReg.ActnCode=ambPrices.Code "
									+ "and (ambReg.SIMPCode=ambPrices.SIMPCode or ambPrices.SIMPCode='88') "
									+ "group by ActnType, ActnCode, PracticeCode, DateFrom, DateTo "
									+ "order by DateFrom, DateTo, ActnType, ActnCode, PracticeCode",
							"Амбул. дейност - спецификации"), };

	private class Parsela {
		private static final int BUFFER = 2048, LEFT_NODE_SEPR = 1,
				RIGHT_NODE_SEPR = 2, META_DATA_SEPR = 4, NOTE_SEPR = 8,
				TAG_SEPR = 16, ATTR_VAL_SEPR = 32, SINGLE_NODE_SEPR = 64,
				ENCLOSING_NODES_SEPR = 128, ATTR = 0, CONTENT = 1, EXTRA = 2,
				BLANK_CHILD = 3, CHILD = 4, END = 5, BLANK_NEXT = 6, NEXT = 7;

		private static final String CHARSET = "utf-8";

		private class Separator {
			public String str;
			public int type = 0;

			public Separator(String str) {
				this.str = str;
			}

			public Separator(String str, int type) {
				this.str = str;
				this.type = type;
			}

			public boolean isType(int type) {
				return (this.type & type) == type;
			}
		}

		private final Separator LTE = new Separator("</", LEFT_NODE_SEPR
				| TAG_SEPR | ENCLOSING_NODES_SEPR), RTS = new Separator("/>",
				RIGHT_NODE_SEPR | TAG_SEPR | SINGLE_NODE_SEPR),
				LMS = new Separator("<?", LEFT_NODE_SEPR | META_DATA_SEPR
						| SINGLE_NODE_SEPR), RMS = new Separator("?>",
						RIGHT_NODE_SEPR | META_DATA_SEPR | SINGLE_NODE_SEPR),
				LNS = new Separator("<!--", LEFT_NODE_SEPR | NOTE_SEPR
						| SINGLE_NODE_SEPR), RNS = new Separator("-->",
						RIGHT_NODE_SEPR | NOTE_SEPR | SINGLE_NODE_SEPR),
				LTSE = new Separator("<", LEFT_NODE_SEPR | TAG_SEPR
						| SINGLE_NODE_SEPR | ENCLOSING_NODES_SEPR),
				RTE = new Separator(">", RIGHT_NODE_SEPR | TAG_SEPR
						| ENCLOSING_NODES_SEPR), LAS = new Separator("=\"",
						LEFT_NODE_SEPR | ATTR_VAL_SEPR | SINGLE_NODE_SEPR),
				RAS = new Separator("\"", RIGHT_NODE_SEPR | ATTR_VAL_SEPR
						| SINGLE_NODE_SEPR), SPACE = new Separator(" ");

		private final Separator[] GENERAL = new Separator[] { this.LTE,
				this.RTS, this.RMS, this.LMS, this.LNS, this.RNS, this.LTSE,
				this.RTE }, LOCAL = new Separator[] { this.LAS, this.RAS };

		private class Joke {
			private Parsela joker;

			private Joke[] root = new Joke[8], top = new Joke[8];

			private Separator on, off;

			private String str;

			public boolean isClosed() {
				Separator x = this.on, y = this.off;
				return x != null
						&& y != null
						&& (x.type & LEFT_NODE_SEPR) == LEFT_NODE_SEPR
						&& (y.type & RIGHT_NODE_SEPR) == RIGHT_NODE_SEPR
						&& (x.type & y.type & (META_DATA_SEPR | NOTE_SEPR
								| TAG_SEPR | ATTR_VAL_SEPR)) != 0
						&& (x.type & y.type & (SINGLE_NODE_SEPR | ENCLOSING_NODES_SEPR)) != 0
						|| x == null && y == null;
			}

			public boolean closes(Joke j) {
				return j != null && j.on == Parsela.this.LTSE
						&& j.off == Parsela.this.RTE
						&& this.on == Parsela.this.LTE
						&& this.off == Parsela.this.RTE
						&& j.str.equals(this.str);
			}

			public boolean repeats(Joke j) {
				return j != null && j.on == this.on && j.off == this.off
						&& j.str.equals(this.str);
			}
		}

		private Joke root, top;

		private class CharsetField {
			public boolean explicit;
			public String original, charset;
		}

		private CharsetField myCharset = new CharsetField();

		private class Delimitors {
			public String tab = "\t", eof = "\r\n";
		}

		private Delimitors myDelims = new Delimitors();

		public Parsela(File f, String db) {
			try {
				if (f.isFile()) {
					File expt = new File(f.getParent(), "controlExport_"
							+ f.getName());
					Report act = new Report();
					read(f);
					act.printTime("Четене на файл: ", 3);
					this.apply();
					act.printTime("Разделяне на възли: ", 3);
					this.close();
					act.printTime("Проверка за четност на разделителите: ", 3);
					prepare();
					act.printTime("Обособяване на атрибутите: ", 3);
					this.plant();
					act.printTime(
							"Обособяване на вложените последователности: ", 3);
					this.content();
					act.printTime(
							"Обособяване на съдържанието и празните възли: ", 3);
					this.export(expt);
					act.printTime("Контролен експорт: ", 3);
					compare(f, expt);
					act.printTime("Сравняване на файловете: ", 3);
					expt.delete();
					act.printTime("Изтриване на контролното копие: ", 3);
					this.copyPasteTagContentToPivotalElement();
					act.printTime("Подготовка за трансфер: ", 3);
					this.transfer();
					act.printTime("Прехвърляне в БД: ", 3);
					act.printTime(f.getName() + ": ", 2);
				}
			} catch (Exception e) {
				this.root = this.top = null;
				e.printStackTrace(System.out);
			}
		}

		private void read(File f) throws Exception {
			this.myCharset.charset = CHARSET;
			InputStreamReader stream = new InputStreamReader(
					new FileInputStream(f), this.myCharset.charset);
			char data[] = new char[BUFFER];
			int count = stream.read(data, 0, BUFFER);
			if (-1 < count) {
				String s = String.copyValueOf(data, 0, count);
				int a = s.indexOf("encoding");
				if (a > -1) {
					int b = s.indexOf("\"", a + 1);
					if (b > -1) {
						int c = s.indexOf("\"", b + 1);
						if (c > -1) {
							s = s.substring(b + 1, c).trim().toLowerCase();
							this.myCharset.explicit = true;
							this.myCharset.original = s;
							if (Charset.isSupported(s)) {
								this.myCharset.charset = s;
								if (!s.equals(CHARSET)) {
									stream.close();
									stream = new InputStreamReader(
											new FileInputStream(f),
											this.myCharset.charset);
									count = stream.read(data, 0, BUFFER);
								}
							}
						}
					}
				}
			}
			while (-1 < count) {
				Joke j = new Joke();
				j.joker = this;
				j.str = String.copyValueOf(data, 0, count);

				if (this.top != null)
					this.top.root[NEXT] = j;
				else
					this.root = j;
				this.top = j;
				count = stream.read(data, 0, BUFFER);
			}
			stream.close();
		}

		private boolean cutAndPasteAt(Joke joke, Separator separator, int index) {
			if (joke != null && joke.joker == this && joke.str != null) {
				int i = joke.str.indexOf(separator.str);
				if (-1 < i) {
					Joke j = new Joke();
					j.joker = this;
					if (index == NEXT) {
						j.str = joke.str.substring(i + separator.str.length());
						j.off = joke.off;
						joke.off = separator;
						j.root[NEXT] = joke.root[NEXT];
					} else {
						j.str = joke.str.substring(i);
					}
					joke.str = joke.str.substring(0, i);
					joke.root[index] = j;
					return true;
				}
			}
			return false;
		}

		private int apply(Joke joke, Separator separator) {
			int cntr = 0;
			do {
				Joke j = joke;
				while (j != null) {
					if (cutAndPasteAt(j, separator, NEXT))
						cntr++;
					j = j.root[NEXT];
				}
			} while (consolidate(joke) > 0);

			if (separator.isType(LEFT_NODE_SEPR))
				return cntr;
			else
				return -cntr;
		}

		private void apply(Joke joke, Separator[] separators) throws Exception {
			int diff = 0;
			for (int i = 0; i < separators.length; i++)
				diff += this.apply(joke, separators[i]);
			if (diff != 0)
				throw new Exception("Различен брой леви и десни разделители.");
		}

		private void apply() throws Exception {
			this.apply(this.root, this.GENERAL);
		}

		private int consolidate(Joke joke) {
			int cntr = 0;
			Joke j = joke;
			while (j != null) {
				if (j.off == null && j.root[NEXT] != null) {
					j.str += j.root[NEXT].str;
					j.off = j.root[NEXT].off;
					j.root[NEXT] = j.root[NEXT].root[NEXT];
					cntr++;
				}

				j = j.root[NEXT];
			}
			return cntr;
		}

		private void close(Joke joke) throws Exception {
			Joke j = joke;
			while (j != null) {
				if (j.off != null && j.off.isType(LEFT_NODE_SEPR)) {
					j.root[NEXT].on = j.off;
					j.off = null;
				} else {
					if (!j.isClosed())
						throw new Exception("Разделители от различен тип: "
								+ j.on.str + ", " + j.off.str);
				}

				j = j.root[NEXT];
			}
		}

		private void close() throws Exception {
			this.close(this.root);
		}

		private void prepare() throws Exception {
			Joke j = this.root;
			while (j != null) {
				if (j.on != null && (j.on == this.LTSE || j.on == this.LMS)
						&& j.str.indexOf(" ") > -1) {
					cutAndPasteAt(j, this.SPACE, ATTR);
					this.apply(j.root[ATTR], this.LOCAL);
					this.close(j.root[ATTR]);
				}

				j = j.root[NEXT];
			}
		}

		private void plant(Joke joke) throws Exception {
			if (joke.on == this.LTSE && joke.off == this.RTE) {
				Joke j = joke, prev = joke;
				int openOns = 1, closeOns = 0;
				while (j.root[NEXT] != null && openOns > closeOns) {
					prev = j;
					j = j.root[NEXT];
					if (j.repeats(joke))
						openOns++;
					if (j.closes(joke))
						closeOns++;
				}
				if (openOns != closeOns)
					throw new Exception("Неправилно затворен елемент.");

				joke.root[CHILD] = joke.root[NEXT];
				joke.root[NEXT] = j.root[NEXT];
				joke.root[END] = j;
				prev.root[NEXT] = null;
				j.root[NEXT] = null;

				j = joke.root[CHILD];
				while (j != null) {
					this.plant(j);
					j = j.root[NEXT];
				}
			}
		}

		private void plant() throws Exception {
			Joke j = this.root;
			while (j != null) {
				this.plant(j);
				j = j.root[NEXT];
			}
		}

		private void content(Joke j) {
			if (j != null) {
				if (j.root[CHILD] != null && j.root[CHILD].on == null
						&& j.root[CHILD].off == null) {
					if (j.root[CHILD].root[NEXT] != null) {
						j.root[BLANK_CHILD] = j.root[CHILD];
						j.root[CHILD] = j.root[CHILD].root[NEXT];
						j.root[BLANK_CHILD].root[NEXT] = null;
					} else {
						j.root[CONTENT] = j.root[CHILD];
						j.root[CHILD] = null;
						j.root[CONTENT].root[NEXT] = null;
					}
				}
				if (j.root[NEXT] != null && j.root[NEXT].on == null
						&& j.root[NEXT].off == null) {
					j.root[BLANK_NEXT] = j.root[NEXT];
					j.root[NEXT] = j.root[NEXT].root[NEXT];
					j.root[BLANK_NEXT].root[NEXT] = null;
				}
				Joke k = j.root[ATTR];
				while (k != null) {
					if (k.on == null && k.off == null && k.root[NEXT] != null) {
						k.root[CONTENT] = k.root[NEXT];
						k.root[NEXT] = k.root[NEXT].root[NEXT];
						k.root[CONTENT].root[NEXT] = null;
					}

					k = k.root[NEXT];
				}

				j = j.root[CHILD];
				while (j != null) {
					this.content(j);
					j = j.root[NEXT];
				}
			}
		}

		private void content() {
			Joke j = this.root;
			while (j != null) {
				this.content(j);
				j = j.root[NEXT];
			}
		}

		private void changeCharset(String charset) {
			if (charset != null && Charset.isSupported(charset)
					&& !charset.equals(this.myCharset.charset)) {
				this.myCharset.charset = charset;
				if (this.myCharset.explicit) {
					Joke j = this.root;
					while (j != null) {
						Joke k = j.root[ATTR];
						while (k != null) {
							if (k.root[CONTENT] != null
									&& k.root[CONTENT].str
											.equals(this.myCharset.original)) {
								k.root[CONTENT].str = charset;
								return;
							}
							k = k.root[NEXT];
						}
						j = j.root[NEXT];
					}
				}
			}
		}

		private void export(Joke j, Writer out) throws Exception {
			if (j != null) {
				if (j.on != null && j.on.str != null)
					out.write(j.on.str);
				if (j.str != null)
					out.write(j.str);
				for (int i = ATTR; i <= NEXT; i++) {
					this.export(j.root[i], out);
					if (i == ATTR && j.off != null && j.off.str != null)
						out.write(j.off.str);
				}
			}
		}

		private void export(File myExp) throws Exception {
			Writer out = new OutputStreamWriter(new FileOutputStream(myExp),
					this.myCharset.charset);
			this.export(this.root, out);
			out.close();
		}

		private void export(Joke j, Writer out, Delimitors d, int level)
				throws Exception {
			if (j != null) {
				int n = 0;
				if (j.on != null && j.on.str != null)
					out.write(j.on.str);
				if (j.str != null)
					out.write(j.str);
				for (int i = ATTR; i <= NEXT; i++) {
					if (i == BLANK_CHILD || i == CHILD)
						n = level + 1;
					else {
						if (i == BLANK_NEXT && j.root[NEXT] == null)
							n = level - 1;
						else
							n = level;
					}
					if (i == BLANK_CHILD || i == BLANK_NEXT) {
						if (j.root[i] != null) {
							out.write(d.eof);
							for (int k = 0; k < n; k++)
								out.write(d.tab);
						}
					} else {
						this.export(j.root[i], out, d, n);
					}
					if (i == ATTR && j.off != null && j.off.str != null)
						out.write(j.off.str);
				}
			}
		}

		private void compare(File myImp, File myExp) throws Exception {
			if (this.myCharset.charset != null && myImp.exists()
					&& myExp.exists()) {
				InputStreamReader uploaded = new InputStreamReader(
						new FileInputStream(myImp), this.myCharset.charset);
				InputStreamReader exported = new InputStreamReader(
						new FileInputStream(myExp), this.myCharset.charset);
				int upChrs, exChrs;
				char a[] = new char[BUFFER], b[] = new char[BUFFER];
				do {
					upChrs = uploaded.read(a, 0, BUFFER);
					exChrs = exported.read(b, 0, BUFFER);

					if (upChrs < exChrs)
						throw new Exception(
								"Разлика в съдържанието: изходeн < краeн файл\n"
										+ "===============\n"
										+ String.copyValueOf(b, upChrs + 1,
												exChrs) + "\n===============\n");
					else if (upChrs > exChrs)
						throw new Exception(
								"Разлика в съдържанието: изходeн > краeн файл\n"
										+ "===============\n"
										+ String.copyValueOf(a, exChrs + 1,
												upChrs) + "\n===============\n");
					else if (!Arrays.equals(a, b))
						throw new Exception(
								"Разлика в съдържанието: изходeн != краeн файл\n"
										+ "===============\n"
										+ String.copyValueOf(a, 0, upChrs)
										+ "\n" + "===============\n"
										+ String.copyValueOf(b, 0, exChrs)
										+ "\n" + "===============\n");
				} while (-1 < upChrs && -1 < exChrs);
				uploaded.close();
				exported.close();
			}
		}

		private boolean copyPasteTagContent(Joke j, Joke pivot) {
			if (j != null && j.joker == this && pivot != null
					&& pivot.joker == this && j.root[CONTENT] != null) {
				Joke joke = new Joke();
				joke.joker = this;
				joke.str = j.str;
				joke.on = this.LTSE;
				joke.off = this.RTE;
				joke.root[CONTENT] = new Joke();
				joke.root[CONTENT].str = j.root[CONTENT].str;
				joke.root[END] = new Joke();
				joke.root[END].str = joke.str;
				joke.root[END].on = this.LTE;
				joke.root[END].off = this.RTE;
				if (pivot.top[EXTRA] != null)
					pivot.top[EXTRA].root[NEXT] = joke;
				else
					pivot.root[EXTRA] = joke;
				pivot.top[EXTRA] = joke;
				return true;
			}
			return false;
		}

		private void copyPasteTagContentToPivotalElement(Joke j, Joke pivot) {
			if (j != null && j.joker == this) {
				for (int x = 0; x < Parselot.this.TABS.length; x++)
					if (j.str.equals(Parselot.this.TABS[x].xmlTag)) {
						pivot = j;
						break;
					}

				copyPasteTagContent(j, pivot);
				Joke k = j.root[CHILD];
				while (k != null) {
					this.copyPasteTagContentToPivotalElement(k, pivot);
					k = k.root[NEXT];
				}
			}
		}

		private void copyPasteTagContentToPivotalElement() {
			Joke j = this.root;
			while (j != null) {
				this.copyPasteTagContentToPivotalElement(j, null);
				j = j.root[NEXT];
			}
		}

		private void transfer(Joke j, Connection conn, int parentKey,
				String parentTag) throws Exception {
			if (j != null && j.joker == this) {
				int myKey = parentKey;
				String myTag = parentTag;
				Joke k = j.root[EXTRA];
				if (k != null) {
					for (int x = 0; x < Parselot.this.TABS.length; x++) {
						if (k.str.equals(Parselot.this.TABS[x].fields[0]
								.getXmlTag())) {
							PreparedStatement pstmt = conn.prepareStatement(
									Parselot.this.TABS[x].sqlIns,
									Statement.RETURN_GENERATED_KEYS);
							PreparedStatement keyStmt = conn
									.prepareStatement(Parselot.this.TABS[x].sqlKeyQry);

							int n = 0, m = 0;
							while (n < Parselot.this.TABS[x].fields.length) {
								n++;
								if (k != null
										&& k.str.equals(Parselot.this.TABS[x].fields[n - 1]
												.getXmlTag())
										&& k.root[CONTENT] != null) {
									Parselot.this.TABS[x].fields[n - 1]
											.setStatement(pstmt, n,
													k.root[CONTENT].str);
									if (m < Parselot.this.TABS[x].keyFields.length
											&& Parselot.this.TABS[x].keyFields[m] == n - 1) {
										m++;
										Parselot.this.TABS[x].fields[n - 1]
												.setStatement(keyStmt, m,
														k.root[CONTENT].str);
									}

									k = k.root[NEXT];
								} else {
									Parselot.this.TABS[x].fields[n - 1]
											.setNull(pstmt, n);
								}
							}

							if (parentTag
									.equals(Parselot.this.TABS[x].fields[n - 2]
											.getXmlTag()))
								pstmt.setInt(n - 1, parentKey);

							if (k != null) {
								pstmt.setString(n, k.str);
								System.out.println("x=" + x + "\tn=" + n + "\t"
										+ k.str);
							} else {
								pstmt.setString(n, "true");

								if (m < Parselot.this.TABS[x].keyFields.length)
									keyStmt.setInt(
											Parselot.this.TABS[x].keyFields.length,
											parentKey);
								keyStmt.executeQuery();
								ResultSet rSet = keyStmt.getResultSet();
								if (!rSet.next()) {
									pstmt.executeUpdate();

									ResultSet keys = pstmt.getGeneratedKeys();
									if (keys.getMetaData().getColumnCount() > 0
											&& keys.next()) {
										myKey = keys.getInt(1);
										myTag = Parselot.this.TABS[x].xmlTag;
									}
								} else {
									myKey = rSet.getInt("id");
									myTag = Parselot.this.TABS[x].xmlTag;
								}
							}
							keyStmt.close();
							pstmt.close();
							break;
						}
					}
				}
				this.transfer(j.root[CHILD], conn, myKey, myTag);
				this.transfer(j.root[NEXT], conn, parentKey, parentTag);
			}
		}

		private void transfer() throws Exception {
			this.transfer(this.root, Parselot.this.conn, 0, "");
		}
	}

	public void export(Parsela p, File myExp, String charset) throws Exception {
		p.changeCharset(charset);
		Writer out = new OutputStreamWriter(new FileOutputStream(myExp),
				p.myCharset.charset);
		p.export(p.root, out, p.myDelims, 0);
		out.close();
	}

	private void setConn() throws Exception {
		Class.forName("com.mysql.jdbc.Driver");
		this.conn = DriverManager
				.getConnection("jdbc:mysql://localhost:3306/"
						+ "?user=root&password=&useUnicode=true&characterEncoding=UTF-8");
		this.stmt = this.conn.createStatement(
				ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
	}

	private void setDataBase(String dbName) throws Exception {
		this.stmt.executeUpdate("drop database if exists " + dbName);
		this.stmt.executeUpdate("create database " + dbName
				+ " default character set utf8 collate utf8_general_ci");
		this.stmt.execute("use " + dbName);
		for (int i = 0; i < this.TABS.length; i++)
			this.stmt.executeUpdate(this.TABS[i].sqlNew);
	}

	private void reg() throws Exception {
		Report act = new Report();
		this.stmt.executeUpdate("delete from ambReg");
		for (int i = 0; i < INS.length; i++) {
			this.stmt.executeUpdate(INS[i].sqlSelect);
			act.printTime(INS[i].sqlTitle + ": " + this.stmt.getUpdateCount()
					+ " реда", 2);
		}
	}

	private void export(File expt, int qry) throws Exception {
		Report act = new Report();
		this.stmt.executeQuery(QRYS[qry].sqlSelect);

		ResultSet rs = this.stmt.getResultSet();
		ResultSetMetaData meta = rs.getMetaData();

		File rsFile = new File(expt, QRYS[qry].sqlTitle + ".html");
		Writer out = new OutputStreamWriter(new FileOutputStream(
				rsFile.getAbsolutePath()));

		String s = "<html><head><title>" + QRYS[qry].sqlTitle
				+ "</title><style>" + this.STYLE + "</style></head>"
				+ "<body><table>\r\n" + "\t<tr><th></th><th colspan=\""
				+ meta.getColumnCount() + "\">" + QRYS[qry].sqlTitle
				+ "</th></tr>\r\n" + "\t<tr><th>No.</th>";
		for (int n = 0; n < meta.getColumnCount(); n++)
			s += "<th>" + meta.getColumnLabel(n + 1) + "</th>";
		s += "</tr>\r\n";
		out.write(s);
		int m = 0;
		while (rs.next()) {
			m++;
			s = "\t<tr><td>" + m + "</td>";
			for (int n = 0; n < meta.getColumnCount(); n++) {
				String str = rs.getString(n + 1);
				s += "<td class=\"x" + meta.getColumnType(n + 1) + "\">"
						+ (str != null ? str : "") + "</td>";
			}
			s += "</tr>\r\n";
			out.write(s);
		}
		s = "</table><ul><li>Справката е изготвена за: "
				+ act.printTime(QRYS[qry].sqlTitle + ": " + m + " реда", 2)
				+ " сек.</li></ul></body></html>";
		out.write(s);
		out.close();
	}

	private void export(File expt) throws Exception {
		for (int i = 0; i < QRYS.length; i++)
			export(expt, i);
	}

	public File dirFrom, dirTo;

	public File[] files;

	public String dBase;

	public void init(String root, String uploaded, String exported, String dBase) {
		try {
			File r = new File(root);
			r.createNewFile();
			this.dirFrom = new File(root, uploaded);
			this.dirFrom.createNewFile();
			this.dirTo = new File(root, exported);
			this.dirTo.createNewFile();
		} catch (Exception e) {
			e.printStackTrace(System.out);
		} finally {
			this.dBase = dBase;
		}
	}

	public void export(File[] files, String db, File dirTo) {
		try {
			Report act = new Report();
			setConn();
			act.printTime("Начало на сесия в СУБД: ", 1);

			setDataBase(db);
			act.printTime("Създаване на БД: ", 1);
			for (File f : files)
				if (f != null)
					new Parsela(f, db);
			act.printTime("Въвеждане в БД: ", 1);

			reg();
			act.printTime("Кодиране на дейностите: ", 1);
			this.export(dirTo);
			act.printTime("Извеждане на справки: ", 1);
			this.stmt.close();
			act.printTime("Край на връзката с БД: ", 1);
			this.conn.close();
			act.printTime("Край на сесията в СУБД: ", 1);
			act.printTime("Общо време: ", 0);
		} catch (Exception e) {
			e.printStackTrace(System.out);
		}
	}
}
