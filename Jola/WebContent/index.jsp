<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ page import="java.io.*"%>
<%@ page import="java.util.*"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>МДД и СИМП</title>
<jsp:useBean id="bn" scope="session" class="laam.Parselot"></jsp:useBean>
<script type="text/javascript">
	function split(s) {
		var pcs = {
			prices : "",
			period : "",
			practice : "",
			uin : "",
			spec : ""
		};
		var x = s.split(".");
		s = x.join("_");
		x = s.split("-");
		s = x.join("_");
		x = s.split("_");
		var once = false;
		for ( var j = 0; j < x.length; j++) {
			if (x[j].indexOf("Prices") == 0) {
				pcs.prices = x[j];
			} else if (x[j].length == 6) {
				pcs.period = x[j];
			} else if (x[j].length == 14) {
				pcs.practice = x[j].slice(0, 10);
				pcs.period = x[j].slice(-4);
				once = true;
			} else if (x[j].length == 10) {
				if (!once) {
					pcs.practice = x[j];
					once = true;
				} else {
					pcs.uin = x[j];
				}
			} else if (x[j].length == 2) {
				pcs.spec = x[j];
			}
		}
		return pcs;
	}
	function checkIfContains(name, obj) {
		var arr = document.getElementsByName(name);
		var pcs = {};
		for ( var i = 0; i < arr.length; i++) {
			pcs = split(arr[i].value);
			if (pcs.prices == obj.value || pcs.period == obj.value
					|| pcs.practice == obj.value || pcs.uin == obj.value
					|| pcs.spec == obj.value)
				arr[i].checked = obj.checked;
		}
	}
	function show(id) {
		var d = document.getElementById(id).childNodes[2];
		if (d.style.display != "none")
			d.style.display = "none";
		else
			d.style.display = "inline";
	}
	function checkboxDiv(optArr, sId, sTxt) {
		optArr.sort();
		var stat = [ 1 ];
		var i = 1;
		while (i < optArr.length) {
			if (optArr[i - 1] == optArr[i]) {
				stat[i - 1]++;
				optArr.splice(i, 1);
			} else {
				stat[i] = 1;
				i++;
			}
		}
		var s = sTxt + " <em onmouseover=\"this.style.color='red'\" "
				+ "onmouseout=\"this.style.color='black'\" onclick=\"show('"
				+ sId + "')\">- подгрупи: " + optArr.length
				+ " бр.</em><div><hr />";
		for ( var i = 0; i < optArr.length; i++)
			s += ("<div class=\"chbx\"><input type=\"checkbox\" name=\"" + sId
					+ "\" value=\"" + optArr[i]
					+ "\" onclick=\"checkIfContains('files',this)\">"
					+ "&nbsp;" + optArr[i] + " <em>- " + stat[i] + " бр.</em>" + "</div>");
		s += "<hr /></div>";
		document.getElementById(sId).innerHTML = s;
	}
	function pushIfLonger(strArr, strElem, strLen) {
		if (strElem.length > strLen)
			strArr.push(strElem);
	}
	function groupBySubstr(name) {
		var arr = document.getElementsByName(name);
		var all = [], prices = [], period = [], practice = [], uin = [], spec = [];
		var pcs = {};
		for ( var i = 0; i < arr.length; i++) {
			pcs = split(arr[i].value);
			pushIfLonger(all, "", -1);
			pushIfLonger(prices, pcs.prices, 0);
			pushIfLonger(period, pcs.period, 0);
			pushIfLonger(practice, pcs.practice, 0);
			pushIfLonger(uin, pcs.uin, 0);
			pushIfLonger(spec, pcs.spec, 0);
		}
		checkboxDiv(all, "all", "всички");
		checkboxDiv(prices, "prices", "цени");
		checkboxDiv(period, "period", "отчетен период");
		checkboxDiv(practice, "practice", "лечебно заведение");
		checkboxDiv(uin, "uin", "лекар с УИН");
		checkboxDiv(spec, "spec", "специалност с код");
	}
</script>
<style>
em {
	display: inline;
	padding: 6pt;
	color: #9988aa;
}

.chbx {
	display: inline-block;
	width: 150pt;
}
</style>
</head>
<body>
	<%
		try {
			bn.init("D:/Java/workspace/JDirs", "uploaded", "exported",
					"labamb");
			String[] total = bn.dirFrom.list(), selected = request
					.getParameterValues("files");
	%>
	<br />&nbsp;Избери / изключи
	<ol>
		<li><div id="all"></div></li>
		<li><div id="prices"></div></li>
		<li><div id="period"></div></li>
		<li><div id="practice"></div></li>
		<li><div id="uin"></div></li>
		<li><div id="spec"></div></li>
	</ol>
	<form action="index.jsp" method="post">
		<%
			for (int i = 0; i < total.length; i++) {
		%>
		<input type="checkbox" name="files" value="<%=total[i]%>">
		&nbsp;<%=(i + 1)%>.&nbsp;&nbsp;&nbsp;<%=total[i]%><br />
		<%
			}
		%><input type="submit" value="Импорт в БД">
	</form>
	<script type="text/javascript">
		groupBySubstr('files');
	</script>
	<%
		if (selected != null) {
				bn.files = new File[selected.length];
				for (int i = 0; i < selected.length; i++)
					bn.files[i] = new File(bn.dirFrom, selected[i]);
				bn.export(bn.files, bn.dBase, bn.dirTo);
			}
		} catch (Exception e) {
			e.printStackTrace(System.out);
		}
	%>
</body>
</html>