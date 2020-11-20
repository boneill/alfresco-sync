package au.org.amc.repo.webscripts.armature;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.executer.ImporterActionExecuter;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.ISO8601DateFormat;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;

import au.org.amc.repo.services.AllServices;
//import au.org.amc.repo.utils.PathCreatorUtil;
import au.org.amc.repo.webscripts.base.BaseWebscriptException;
import au.org.amc.repo.webscripts.base.MultipartWebscriptService;
import au.org.amc.repo.webscripts.base.ValidationException;
import  org.alfresco.repo.site.SiteDoesNotExistException;
import org.alfresco.service.cmr.action.Action;

public class CreateArmatureDocumentWebscript implements MultipartWebscriptService {

  private static final Logger logger = Logger.getLogger(CreateArmatureDocumentWebscript.class);
  private NodeService nodeService;
  private FileFolderService fileFolderService;
  
  private Map<String, QName> normalInput = new HashMap<>();
  private Map<String, QName> dateInput = new HashMap<>();
  
  
  private NodeRef folderNodeRef;
  private String planFolderPath;
  private ContentService contentService;
  private SiteService siteService;
  protected MimetypeService mimetypeService;
  
  
  private String armatureSiteShortName;
  private ActionService actionService; 
  
  final static String DOCUMENT_TYPE_REPORT = "pdf";
  final static String DOCUMENT_TYPE_ATTACHMENTS = "zip";
  final static String DOCUMENT_CREATOR = "AMS";
  
  

  public SiteService getSiteService() {
    return this.siteService;
  }


  public void setSiteService(SiteService siteService) {
    this.siteService = siteService;
  }


  public void init() {

    normalInput.put("name", ContentModel.PROP_NAME);
    normalInput.put("documentType", ContentModel.PROP_TITLE);
    
  }
  
  
  /**
   * Get all the values that are passed in and store htem in a map.
   * 
   * @param properties
   * @return
   * @throws ValidationException
   */
  private Map<QName, Serializable> getProperties(Map<String, String> properties) throws ValidationException {
    Map<QName, Serializable> map = new HashMap<>();
    for (String key : normalInput.keySet()) {
      QName qName = normalInput.get(key);
      if (properties.containsKey(key)) {
        String value = properties.get(key);
        //decode value
        String decodedValue = decode(value);
        //map.put(key, decodeValue);       
        map.put(qName, decodedValue);
      }
    }
    
    for (String key : dateInput.keySet()) {
      QName qName = dateInput.get(key);
      if (properties.containsKey(key)) {
        String value = properties.get(key);
        Date date = ISO8601DateFormat.parse(value);
        
        map.put(qName, date);
      }
    }
    
    //Check title and truncate if > 245
    String title = (String) map.get(ContentModel.PROP_TITLE);
    //limit to 245 characters
    if(title != null && title.length() > 245){
      //truncate
      title = StringUtils.left(title, 245);
      map.put(ContentModel.PROP_TITLE, title);
    }
  
    return map;
  }
  
  /**
   * Decode values for utf8.
   * 
   * @param value
   * @return
   * @throws ValidationException
   */
  private String decode(String value) throws ValidationException {
    String decoded = value;
    try {
      decoded = URLDecoder.decode(value, StandardCharsets.UTF_8.toString());
    } catch (UnsupportedEncodingException e) {
      String msg = String.format("Parameter: %s could not be decoded", value);
      throw new ValidationException(msg, e);
    } catch (Exception e) {
      String msg = String.format("Parameter: %s could not be decoded", value);
      throw new ValidationException(msg, e);
    }
    return decoded;
  }
  
  
  /**
   * Create the required document
   * 
   * @param folder
   * @param filename
   * @param properties
   * @return
   */
  private NodeRef createDocument(NodeRef folder, String filename, Map<QName, Serializable> properties){
    
    logger.debug("Entered CreateArmatureDocumentWebscript createDocument method");
    
    NodeRef nodeRef = null;
    String newFilename = filename;
    int i = 0;
    do {
      newFilename = getFileNameVersion(filename, i);
      nodeRef = nodeService.getChildByName(folder, ContentModel.ASSOC_CONTAINS, newFilename);
      i++;
    } while (nodeRef != null);
    //set name
    properties.put(ContentModel.PROP_NAME, newFilename);
    properties.put(ContentModel.PROP_CREATOR, DOCUMENT_CREATOR);
    
    //get SequenceNumber and set
    
    
    ChildAssociationRef childAssociationRef = nodeService.createNode(folder, ContentModel.ASSOC_CONTAINS,
        QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, newFilename),
        ContentModel.TYPE_CONTENT,
        properties);
    
    logger.debug("CreateArmatureDocumentWebscript createDocument: Created document " + newFilename);
    
    logger.debug("Exited CreateArmatureDocumentWebscript createDocument method ");
    return childAssociationRef.getChildRef();
  }
  
  
  /**
   * Add number value to the existing file name.  This is a utility function used to determine the correct name 
   * for a file if the file name being created is a duplicate in the target folder.
   *
   * @param filename
   * @param number
   * @return
   */
  public String getFileNameVersion(String filename, int number) {
    if (number == 0) {
      return filename;
    }
    int position = filename.lastIndexOf(".");
    if (position == -1) {
      position = filename.length();
    }
    String name  = filename.substring(0, position);
    String extension = filename.substring(position);
    
    return String.format("%s_%d%s", name, number, extension);
  }
  
  
  private void addContent(NodeRef document, InputStream content,String fileName) {
    logger.debug("Entered CreateArmatureDocumentWebscript addContent method" ); 
    ContentWriter writer = contentService.getWriter(document, ContentModel.PROP_CONTENT, true);
    
    // get mimetype from object name
    String docMimetype = mimetypeService.guessMimetype(fileName);
    
    if (docMimetype != MimetypeMap.MIMETYPE_BINARY) {
      logger.debug("Mimetype determined as " + docMimetype);
      writer.setMimetype(docMimetype);
    }
    
    // add the content regardless
    if (logger.isDebugEnabled()) logger.debug("Writing content to node");
    writer.putContent(content);
    
    //String docMimetype = mimetypeService.guessMimetype(fileName, content);
    if (docMimetype == MimetypeMap.MIMETYPE_BINARY) {
      
      logger.debug("Need to guess mimetype based on content and filename");
      docMimetype = mimetypeService.guessMimetype(fileName, content);
      logger.debug("Mimetype guessed based on content " + docMimetype);
      if(docMimetype != null){
        ContentWriter newwriter = contentService.getWriter(document, ContentModel.PROP_CONTENT, true);  
        newwriter.setMimetype(docMimetype);
      }
    }
        
    logger.debug("Exited CreateArmatureDocumentWebscript addContent method"); 
  }
  
  private void successOutput(OutputStream outputStream, Serializable nodeRef) throws IOException {
    JsonFactory jasonFactory = new JsonFactory();
    JsonGenerator jsonGenerator = jasonFactory.createJsonGenerator(outputStream, JsonEncoding.UTF8);
    jsonGenerator.writeStartObject();
    jsonGenerator.writeStringField("msg", "document upload success");
    jsonGenerator.writeStringField("NodeRef", nodeRef.toString()); //todod
    jsonGenerator.writeEndObject();
    jsonGenerator.close();
  }

  @Override
  public void run(OutputStream outputStream, String filename, InputStream content, Map<String, String> properties) throws BaseWebscriptException, ValidationException{
    try {
      
      logger.debug("Entered CreateArmatureDocumentWebscript run method");
      String name = properties.get("name");
      String documentType = properties.get("documentType");
      logger.debug("CreateArmatureDocumentWebscript run: - documentType = " + documentType + " name = " + name );
      
      //
      Map<QName, Serializable> props = getProperties(properties);
      
      NodeRef targetFolderRef = getTargetFolder(name, documentType);
      
      NodeRef nodeRef = createDocument(targetFolderRef, name, props);     
      addContent(nodeRef, content, filename);
      
      if(this.DOCUMENT_TYPE_ATTACHMENTS.equals(documentType)){
        // call the unzip action
        this.unzipFile(nodeRef, targetFolderRef);
       
      }
      
      
      
      successOutput(outputStream, nodeRef);
    } catch (IOException  e) {
      throw new BaseWebscriptException(HttpStatus.SC_INTERNAL_SERVER_ERROR);
    } catch (ValidationException e){
      throw new BaseWebscriptException("Bad Encoding" + e.getMessage(), HttpStatus.SC_BAD_REQUEST);
    } catch (AlfrescoRuntimeException e){
      throw new BaseWebscriptException(e.getCause().getMessage(), HttpStatus.SC_BAD_REQUEST);
    } catch (FileNotFoundException fnfe){
      throw new BaseWebscriptException(fnfe.getCause().getMessage(), HttpStatus.SC_NOT_FOUND);
    } catch (Exception  e) {
      logger.error(e.getMessage() , e);
      throw e;
    }
  }
  
  
  /**
   * 
   */
  public void unzipFile(NodeRef zipRef, NodeRef targetFolderRef) {
    
    boolean executeAsync = true;
    
    Map<String, Serializable> aParams = new HashMap<String, Serializable>();
    
    aParams.put("destination", targetFolderRef);
    aParams.put("encoding", "UTF-8");
    

    Action action = actionService.createAction(ImporterActionExecuter.NAME, aParams);
    if (action != null) {
       actionService.executeAction(action, zipRef, true, executeAsync);
    } else {
       throw new RuntimeException("Could not create Import action");
    }
}   
  

  /**
   * Get the target folder that we are creating content in.  
   * If we are creating a Zip (Attachement) file then the target folder will be attachemnt/<name><number>.   
   * ie it will create  a zip folder and number it if it is a duplicate to an eisting named folder. 
   * 
   * @param serializable
   * @return
   * @throws FileNotFoundException 
   */
  private NodeRef getTargetFolder(String name, String documentType) throws FileNotFoundException {
    
    logger.debug("Entered CreateArmatureDocumentWebscript getTargetFolder method for site " + this.getArmatureSiteShortName() + " with documentType value of " + documentType);
    NodeRef targetFolderRef = null;
    try
    {
      NodeRef docLibraryRef = this.getSiteService().getContainer(this.getArmatureSiteShortName(), SiteService.DOCUMENT_LIBRARY);
      
      if(docLibraryRef == null ){
        throw new FileNotFoundException("Could not find the Document Library for Site " + this.getArmatureSiteShortName());
      }
      
      logger.debug("CreateArmatureDocumentWebscript getTargetFolder Site Folder found");
      
      
      
      String targetFolderName = this.DOCUMENT_TYPE_REPORT.equalsIgnoreCase(documentType)? "Progress Reports" : "Attachments";
      
      if(docLibraryRef != null){
        
        targetFolderRef = this.nodeService.getChildByName(docLibraryRef, ContentModel.ASSOC_CONTAINS, targetFolderName);
        
        if(targetFolderRef == null){
          
          targetFolderRef = this.fileFolderService.create(docLibraryRef, targetFolderName, ContentModel.TYPE_FOLDER).getNodeRef();
          
          if(targetFolderRef == null ){
            throw new FileNotFoundException("Could not find target folder " + targetFolderName + " in site " + this.getArmatureSiteShortName());
          }
        }
        
        
        logger.debug("CreateArmatureDocumentWebscript getTargetFolder Top Level Target Folder found");
        
    
        if(this.DOCUMENT_TYPE_ATTACHMENTS.equals(documentType)){
            // for zip folders the target folder needs to be created which will be a subfolder of the attachments folder ,ie /doclib/attachments/<name>
            NodeRef zipFolder = null;        
            int counter = 0;
            String folderName = name;
            while(this.nodeService.getChildByName(targetFolderRef, ContentModel.ASSOC_CONTAINS, folderName) != null){
              folderName = name + (++counter);
            }
            
            targetFolderRef = this.fileFolderService.create(targetFolderRef, folderName, ContentModel.TYPE_FOLDER).getNodeRef();
        }
        
      }
    }
    catch(SiteDoesNotExistException e){
      logger.debug("CreateArmatureDocumentWebscript getTargetFolder " + e.getMessage());
      throw new FileNotFoundException(e.getMessage());
    }
   
    // TODO Auto-generated method stub
    return targetFolderRef;
  }

  
  public void setAllServices(AllServices allServices) {
    nodeService = allServices.getNodeService();
    contentService = allServices.getContentService();
    siteService = allServices.getSiteService();
    mimetypeService = allServices.getMimetypeService();
    fileFolderService = allServices.getFileFolderService();
    this.actionService = allServices.getActionService();

  }
  
  public String getArmatureSiteShortName() {
    return armatureSiteShortName;
  }


  public void setArmatureSiteShortName(String armatureSiteShortName) {
    this.armatureSiteShortName = armatureSiteShortName;
  }
  

  }