package au.org.amc.seedim.action;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.admin.SysAdminParams;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.UrlUtil;
import org.apache.log4j.Logger;
import org.apache.poi.common.usermodel.Hyperlink;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFHyperlink;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class CreatePowerUserReport extends ActionExecuterAbstractBase{

  
  static final Logger logger = Logger.getLogger(CreatePowerUserReport.class);
  
  private SiteService siteService;
  private NodeService nodeService;
  private ContentService contentService;
  private SearchService searchService;
  
  private SysAdminParams sysAdminParams;
  
  public String reportFolderPath = null;
  
  @Override
  protected void executeImpl(Action action, NodeRef actionedUponNodeRef) {
    // TODO Auto-generated method stub
    
    if(logger.isDebugEnabled()){logger.debug("Entered CreatePowerUserReport executeImpl method");}
    
    int siteCount = 0;
    Map< Integer, CreatePowerUserReportDAO> powerUserReportMap = new HashMap<Integer, CreatePowerUserReportDAO>();
    
    //Get All sites
    
    List<SiteInfo> sites= siteService.findSites(null,null,0);
    
    if(!sites.isEmpty()){
        if(logger.isDebugEnabled()){logger.debug("Iterating through sites");}
        
        for (SiteInfo site : sites){
          
          try{
          
            String siteShortName = site.getShortName();
            String siteTitle = site.getTitle();      
            //List<String> siteGroup = siteService.getSiteRoles(siteShortName);
            //if(logger.isDebugEnabled()){logger.debug("siteShortName: " + siteShortName);}
            //if(logger.isDebugEnabled()){logger.debug("siteTitle: " + siteTitle);}
            //if(logger.isDebugEnabled()){logger.debug("siteGroup: " + siteGroup.toString());}
            
            Map<String,String> powerUserMap = siteService.listMembers(siteShortName,null,"SitePowerUser",0);
            String powerUserString = "";
            if(!powerUserMap.isEmpty()){
              powerUserString = powerUserMap.keySet().toString();
              if(logger.isDebugEnabled()){logger.debug("PowerUser" + powerUserString);}
            }
            
            CreatePowerUserReportDAO createPowerUserReportDAO = new CreatePowerUserReportDAO (siteShortName,siteTitle,powerUserString);
            if (logger.isDebugEnabled()) logger.debug("Adding createPowerUserReportDAO item " + createPowerUserReportDAO.toString());
            powerUserReportMap.put(siteCount, createPowerUserReportDAO);
            siteCount++;
          }catch(Exception e){
            if(logger.isDebugEnabled()){logger.debug("Create Power User Report Exception" + e);}
          }
        }
        
        NodeRef reportFolder = null;
        
        if(!powerUserReportMap.isEmpty()){
          //Get Report Folder
          if(logger.isDebugEnabled()){logger.debug("Get Report Folder");}
          reportFolder =  getReportFolder(this.reportFolderPath);
          //Generate report
          if(reportFolder != null && nodeService.exists(reportFolder)){
            if(logger.isDebugEnabled()){logger.debug("Generate report");}
            generateReport(powerUserReportMap,reportFolder);
          }
        }else{
          if(logger.isDebugEnabled()){logger.debug("powerUserReportMap of DAO objects empty");}
        }
        
    }else{
      if(logger.isDebugEnabled()){logger.debug("Sites not FOUND");}
    }
    
        
    if(logger.isDebugEnabled()){logger.debug("Exited CreatePowerUserReport executeImpl method");}
    
  }

  
  protected NodeRef generateReport(Map< Integer, CreatePowerUserReportDAO> createPowerUserReportMap, NodeRef reportFolder){

    if (logger.isDebugEnabled()) logger.debug("Entered CreatePowerUserReport generateReport Method");
    NodeRef reportNode = null;
    Date dNow = new Date( );
    SimpleDateFormat ft = new SimpleDateFormat ("yyyy-MM-dd-hh-mm-ss");
    String fileName = "PowerUserReport" + "-" + ft.format(dNow) + ".xlsx";
    Map<QName, Serializable> props = new HashMap<QName, Serializable>(1);
    props.put(ContentModel.PROP_NAME, fileName);
    //create Content node
    reportNode = this.nodeService.createNode(
            reportFolder,
            ContentModel.ASSOC_CONTAINS,
            QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, fileName),
            ContentModel.TYPE_CONTENT,
            props).getChildRef();
    if (logger.isDebugEnabled()) logger.debug("Power User Report Node Created");

    // Create an XLSX workbook instance
    XSSFWorkbook workbook = new XSSFWorkbook();
    //Create an XLSX sheet
    XSSFSheet sheet = workbook.createSheet("PowerUser Report");

    // create a cell style for hyperlink
    CreationHelper createHelper = workbook.getCreationHelper();
    XSSFCellStyle hlinkstyle = workbook.createCellStyle();
    XSSFFont hlinkfont = workbook.createFont();
    hlinkfont.setUnderline(XSSFFont.U_SINGLE);
    hlinkfont.setColor(HSSFColor.BLUE.index);
    hlinkstyle.setFont(hlinkfont);
    
    
    // set the header row
    setHeaderRow(sheet);

    Set<Integer> keyset = createPowerUserReportMap.keySet();
    int rownum = 1;
    for (int key : keyset) {

        if (logger.isDebugEnabled()) logger.debug("Sites Row " + key);

        //Create A new ROW for each Site
        Row row = sheet.createRow(rownum++);
        CreatePowerUserReportDAO createPowerUserReportDAO = createPowerUserReportMap.get(key);
        int cellnum = 0;
        //Create a new CELL for each Property

        Cell cell = row.createCell(cellnum++);
        cell.setCellValue(createPowerUserReportDAO.getSiteTitle());
        
        cell = row.createCell(cellnum++);
        cell.setCellValue(createPowerUserReportDAO.getSiteShortname());
        
        //Make site shortname a hyperlink
        XSSFHyperlink link = (XSSFHyperlink)createHelper
        .createHyperlink(Hyperlink.LINK_URL);
        
        String url = getSiteUrl(createPowerUserReportDAO.getSiteShortname(),"site-members");
        //link.setAddress("http://www.tutorialspoint.com/" );
        link.setAddress(url);
        cell.setHyperlink((XSSFHyperlink) link);
        cell.setCellStyle(hlinkstyle);
        
        cell = row.createCell(cellnum++);
        //TODO Display Powerusers correctly
        
        String powerUsers = createPowerUserReportDAO.getPowerUsers();
        if(!powerUsers.isEmpty() || powerUsers != null){
          powerUsers = powerUsers.replaceAll(",", " | ").replaceAll("[\\[\\]]", "");
        }
        cell.setCellValue(powerUsers);

    }
    if (logger.isDebugEnabled()) logger.debug("Workbook  Created");
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try {
        if (logger.isDebugEnabled()) logger.debug("Writing workbook to Outputstream");
        workbook.write(out);
        byte[] binaryData = out.toByteArray();
        // Use the content service to set the content onto the newly created node
        ContentWriter writer = this.contentService.getWriter(reportNode, ContentModel.PROP_CONTENT, true);
        writer.setMimetype(MimetypeMap.MIMETYPE_OPENXML_SPREADSHEET);
        //writer.setEncoding("UTF-8");
        //writer.putContent(text);
        if (logger.isDebugEnabled()) logger.debug("Writing content to node");
        writer.putContent(new ByteArrayInputStream(binaryData));
    } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }finally{
        try {
            out.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }        
    }
    if (logger.isDebugEnabled()) logger.debug("Power User Report Created Successfully");
    if (logger.isDebugEnabled()) logger.debug("Exited CreatePowerUserReport generateReport Method");
    
    return reportNode;
    
  }
  
  /**
   * Set Report header
   * @param sheet
   */
  
  private void setHeaderRow(XSSFSheet sheet) {
    // TODO Auto-generated method stub
        XSSFRow row = sheet.createRow(0);

        int cellnum = 0;
        //Create a new CELL for each Property

        XSSFCell cell = row.createCell(cellnum++);
        cell.setCellValue("Site Title");

        cell = row.createCell(cellnum++);
        cell.setCellValue("Site Shortname");
        
        cell = row.createCell(cellnum++);
        cell.setCellValue("Power Users");

  }
  
  
  protected NodeRef getReportFolder(String reportFolderPath ){
    
    
    //Get Report Folder
    StoreRef storeRef = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");
    ResultSet reportFolderRs = searchService.query(storeRef, SearchService.LANGUAGE_XPATH, reportFolderPath);
    
    NodeRef reportFolder = null;
    
    if (reportFolderRs == null || reportFolderRs.length() < 1) {
      
      if (logger.isDebugEnabled()) logger.debug("Unable to locate Report Folder");
    } 
    else {
      reportFolder = reportFolderRs.getNodeRef(0);
      if (logger.isDebugEnabled()) logger.debug("Report Folder found");

    }
    return reportFolder ;
  }
  
  protected String getSiteUrl(String siteShortname, String sitepage){
    
    final String shareUrl = UrlUtil.getShareUrl(sysAdminParams);
    //https://doc.amc.local/share/page/site/Records-and-Info-Mgt/site-members
    //String alfrescoProtocol = sysAdminParams.getAlfrescoProtocol();
    //String alfrescoHost = sysAdminParams.getAlfrescoHost();
    //int sharePort = sysAdminParams.getSharePort();
    
    //String url = alfrescoProtocol + "://" + alfrescoHost + ":" + sharePort + "/page/site/" + siteShortname + "/" + sitepage;
    String url = shareUrl + "/page/site/" + siteShortname + "/" + sitepage;
    if (logger.isDebugEnabled()) logger.debug("URL: " + url);
    return url;
  }
  @Override
  //Dummy parameter had to be added or else the action is not called by the scheduled job
  protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
    // TODO Auto-generated method stub
    paramList.add(new ParameterDefinitionImpl("a-parameter", DataTypeDefinition.TEXT, false, getParamDisplayLabel("a-parameter"))); 
  }

  public SiteService getSiteService() {
    return siteService;
  }

  public void setSiteService(SiteService siteService) {
    this.siteService = siteService;
  }


  public NodeService getNodeService() {
    return nodeService;
  }


  public void setNodeService(NodeService nodeService) {
    this.nodeService = nodeService;
  }


  public ContentService getContentService() {
    return contentService;
  }


  public void setContentService(ContentService contentService) {
    this.contentService = contentService;
  }


  public String getReportFolderPath() {
    return reportFolderPath;
  }


  public void setReportFolderPath(String reportFolderPath) {
    this.reportFolderPath = reportFolderPath;
  }


  public SearchService getSearchService() {
    return searchService;
  }


  public void setSearchService(SearchService searchService) {
    this.searchService = searchService;
  }


  public SysAdminParams getSysAdminParams() {
    return sysAdminParams;
  }


  public void setSysAdminParams(SysAdminParams sysAdminParams) {
    this.sysAdminParams = sysAdminParams;
  }

}
