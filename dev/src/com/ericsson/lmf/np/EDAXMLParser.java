package com.ericsson.lmf.np;

import com.distocraft.dc5000.etl.parser.Main;
import com.distocraft.dc5000.etl.parser.MeasurementFile;
import com.distocraft.dc5000.etl.parser.Parser;
import com.distocraft.dc5000.etl.parser.SourceFile;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class EDAXMLParser implements Parser {

	private int count = 0;
	Map<String, MeasurementDataWrapper> dataHolder = new HashMap<String, MeasurementDataWrapper>();
	private int status = 0;

	private Logger logger;

	private final Map<String, MeasurementFile> openFiles = new HashMap<String, MeasurementFile>();

	private String techPack;

	private String setType;

	private String setName;

	private String workerName;

	private SourceFile sf;

	private Main main;
	private String tagid;

	public void init(Main main, String techPack, String setType, String setName, String workerName) {
		this.main = main;
		this.workerName = workerName;
		this.techPack = techPack;
		this.setType = setType;
		this.setName = setName;
		this.logger = Logger.getLogger("etl." + techPack + "." + setType + "." + setName + ".parser");
		this.logger.info("init()");
	}

	public void parse(SourceFile sf, String techPack, String setType, String setName) throws Exception {
		this.sf = sf;
		this.logger.fine("EDAXMLParser is doing parsing");
		EDAParse(false);
	}

	public int status() {
		return this.status;
	}

	public void run() {
		try {
			this.status = 2;
			SourceFile sf = null;
			while ((sf = this.main.nextSourceFile()) != null) {
				try {
					this.main.preParse(sf);
					parse(sf, this.techPack, this.setType, this.setName);
					this.main.postParse(sf);
				} catch (Exception e) {
					this.main.errorParse(e, sf);
				} finally {
					this.main.finallyParse(sf);
					this.openFiles.clear();
				}
			}
		} catch (Exception e) {
			this.logger.log(Level.WARNING, "Worker parser failed to exception", e);
		} finally {
			this.status = 3;
		}
	}

	private MeasurementFile getMeasurementFile(String tag) {
		MeasurementFile m = null;
		try {
			m = this.openFiles.get(tag);
			if (m == null) {
				m = Main.createMeasurementFile(this.sf, tag, this.techPack, this.setType, this.setName, this.workerName,
						this.logger);
				this.openFiles.put(tag, m);
			}
		} catch (Exception e) {
			this.logger.log(Level.WARNING, "Unable to create measurement file", e);
		}
		return m;
	}

	private void EDAParse(boolean isTest) throws Exception {
		String filePath = "/parser/location_cache.xml_002.xml";
		Pattern underScorePtrn = Pattern.compile("_");
		Pattern commaPtrn = Pattern.compile(",");
		Pattern collonPtrn = Pattern.compile(":");
		Pattern hyphenPtrn = Pattern.compile("-");
		Pattern timezonePtrn = Pattern.compile(".+([\\+\\-][:0-9]+)$");
		Pattern ropPtrn = Pattern.compile("^PT([0-9]+)S$");
		Pattern servicePtrn = Pattern.compile("^[^_]+_(.+)\\..+");
		Pattern servicePartPtrn = Pattern.compile("_.+\\.");
		Map<String, String> commonKeys = new HashMap<String, String>();
		if (!isTest) {
			commonKeys.put("DIRNAME", this.sf.getDir());
			commonKeys.put("filename", this.sf.getName());
			commonKeys.put("DC_SOURCE", "ENP");
		}
		Map<String, String> measKeys = new HashMap<String, String>();
		Map<Integer, String> counterNames = new HashMap<Integer, String>();
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = null;
		if (!isTest) {
			doc = dBuilder.parse(this.sf.getFileInputStream());
		} else {
			doc = dBuilder.parse(new File(filePath));
		}
		doc.getDocumentElement().normalize();
		String root = doc.getDocumentElement().getNodeName();
		NodeList childNodes = doc.getChildNodes().item(0).getChildNodes();
//		NodeList measDataNodeList = (childNodes);
		for (int j = 0; j < childNodes.getLength(); j++) {
			Node node = childNodes.item(j);
//			if (node.getNodeName().equals("SGSNNumber") || node.getNodeName().equals("VLRAddress")) {
//				Node nNode = measDataNodeList.item(j);
				Element e = null;
				if (node.getNodeName().equals("SGSNNumber")) {
					String tagid="SGSNNumber";
					e = (Element) node;
					String[] meArr = commaPtrn.split(e.getElementsByTagName("addr").item(0).getTextContent());
					if (meArr != null) {
						measKeys.put("SGSNNumber_addr", meArr[0]);
					}
					String[] meArr1 = commaPtrn.split(e.getElementsByTagName("nsub").item(0).getTextContent());
					if (meArr1 != null) {
						measKeys.put("SGSNnsub", meArr1[0]);
					}
					addMeasData(measKeys, commonKeys, tagid);
				} else if (node.getNodeName().equals("VLRAddress")) {
					String tagid="VLRAddress";
					e = (Element) node;
					String[] meArr = commaPtrn.split(e.getElementsByTagName("addr").item(0).getTextContent());
					if (meArr != null) {
						measKeys.put("VLR_addr", meArr[0]);
					}
					String[] meArr1 = commaPtrn.split(e.getElementsByTagName("nsub").item(0).getTextContent());
					if (meArr1 != null) {
						measKeys.put("VLRnsub", meArr1[0]);
					}
					addMeasData(measKeys, commonKeys,tagid);
				}
//			}
//    	      System.out.println(measKeys); 
		}

	}
	
	private void addMeasData(Map<String, String> measKeys, Map<String, String> commonKeys, String tagid) throws Exception {
//		for (String dataKey : measKeys.keySet()) {
		System.out.println("measKey --> " +measKeys);
			MeasurementDataWrapper data = new MeasurementDataWrapper(measKeys);
//			if (data == null) {
//	            if (!isTest)
				data.addData(commonKeys);
				measKeys.clear();
//				dataHolder.put(dataKey, data);
//				System.out.println("dataholder --> " + dataHolder);
	          MeasurementFile mFile = getMeasurementFile(tagid);
          mFile.addData(data.getData());
          mFile.saveData();
//			}

//		}
	}

	public static void main(String[] args) throws Exception {
		(new EDAXMLParser()).EDAParse(true);
	}
}
