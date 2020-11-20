package au.com.seedim.synch.repo.services;

import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.node.SystemNodeUtils;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.version.VersionModel;
import org.alfresco.service.cmr.dictionary.AspectDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.lock.LockService;
import org.alfresco.service.cmr.lock.LockStatus;
import org.alfresco.service.cmr.lock.LockType;
import org.alfresco.service.cmr.model.FileExistsException;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.CopyService;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.QueryConsistency;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.cmr.version.VersionType;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.GUID;
import org.apache.log4j.Logger;

import au.com.seedim.synch.model.SeedSynchModel;


public class SynchService {
  
  private static final Logger logger = Logger.getLogger(SynchService.class);
  private SearchService searchService;
  private NamespaceService  namespaceService;
  private NodeService nodeService;
  private Repository repositoryHelper;
  private FileFolderService fileFolderService;
  private BehaviourFilter behaviourFilter;
  private DictionaryService dictionaryService;
  private VersionService versionService;
  private CopyService copyService;
  private ContentService contentService;
  private MimetypeService mimetypeService;
  private LockService lockService;
  
  public Boolean found;
  public List<String> propertiesToTrack;
  
  
  public List<String> getPropertiesToTrack() {
    return propertiesToTrack;
  }

  public void setPropertiesToTrack(List<String> propertiesToTrack) {
    this.propertiesToTrack = propertiesToTrack;
  }
  
  
  
  /**
   * Get SynchSourceRootFolderNodes
   * @return List<NodeRef>
   */
    public List<NodeRef> getAllSynchSourceRootFolders() {
    
    
      if(logger.isDebugEnabled()) {logger.debug("SynchService.getAllSynchSourceRootFolders entered");}
      
      List<NodeRef> synchSourceRootFoldersList = new ArrayList<NodeRef>();
      ResultSet resultSet = null;
      
      try{
      
      final StringBuffer query = new StringBuffer("TYPE:\"cm:folder\" AND ASPECT:\"synch:synchSourceAspect\"");
      
      
        
      
        if(logger.isDebugEnabled()) {logger.debug("Find Synch Source Nodes search query " + query.toString());}
        
        StoreRef storeRef = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");
    
        SearchParameters searchParameters = new SearchParameters();
        searchParameters.addStore(storeRef);
        searchParameters.setLanguage(SearchService.LANGUAGE_FTS_ALFRESCO);
        searchParameters.setQuery(query.toString());
        resultSet = this.searchService.query(searchParameters);
  
        synchSourceRootFoldersList = resultSet.getNodeRefs();
      
        } catch(Exception e) {
        
          if(logger.isDebugEnabled()) {logger.debug("Error finding Synch Source Folders:" + e.getMessage());}
          throw e;
        
      }finally{
        
        if(logger.isDebugEnabled()) {logger.debug("SynchService.getAllSynchSourceRootFolders Synch Source Folders Found ." + resultSet!=null? resultSet.length() : 0);}
        if(logger.isDebugEnabled()) {logger.debug("SynchService.getAllSynchSourceRootFolders exited.");}
        
      }
    
    return synchSourceRootFoldersList;
  }
    
    /**
     * Get SynchMemberNodes
     * @return List<NodeRef>
     */
   public ResultSet getAllSynchMemberNodes(String ssdId) {
   
     List<NodeRef> synchMemberNodesList = new ArrayList<NodeRef>();
      
     final StringBuffer query = new StringBuffer("ASPECT:\"synch:synchMemberNodeAspect\" AND =synch\\:synchSetDefID:\"" +ssdId + "\"");
      
     ResultSet resultSet = null;
        
     try{
        if(logger.isDebugEnabled()) {logger.debug("Find Synch Member Nodes search query " + query.toString());}
        
        StoreRef storeRef = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");
    
        SearchParameters searchParameters = new SearchParameters();
        searchParameters.addStore(storeRef);
        searchParameters.setLanguage(SearchService.LANGUAGE_FTS_ALFRESCO);
        searchParameters.setQuery(query.toString());
        
        resultSet = this.searchService.query(searchParameters);

        synchMemberNodesList = resultSet.getNodeRefs();
      
      } catch(Exception e) {
        
         if(logger.isDebugEnabled()) {logger.debug("Error finding Synch Member Nodes: " + e.getMessage());}
         throw e;
       
     }
      
     return resultSet;
    }
   

   /**
    * Get UnSynchMemberNodes
    * @return resultSet
    */
  public ResultSet getUnSynchedMemberNodes(String ssdId, Boolean hasPropertiesChanged, Boolean hasContentChanged) {
  
    //List<NodeRef> synchMemberNodesList = new ArrayList<NodeRef>();
    Boolean isSynched = false;
         
    final StringBuffer query = new StringBuffer("ASPECT:\"synch:synchMemberNodeAspect\" AND =synch\\:synchSetDefID:\"" +
    ssdId + "\" AND =synch\\:isSynched:\"" +isSynched + "\" AND =synch\\:hasPropertiesChanged:\""+ 
    hasPropertiesChanged + "\" AND =synch\\:hasContentChanged:\""+ hasContentChanged + "\"");
     
    ResultSet resultSet = null;
       
    try{
       if(logger.isDebugEnabled()) {logger.debug("SyncService.getUnSynchedMemberNodes: Find UnSynched Member Nodes search query " + query.toString());}
       
       StoreRef storeRef = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");
   
       SearchParameters searchParameters = new SearchParameters();
       searchParameters.addStore(storeRef);
       searchParameters.setLanguage(SearchService.LANGUAGE_FTS_ALFRESCO);
       searchParameters.setQuery(query.toString());
       searchParameters.setQueryConsistency(QueryConsistency.TRANSACTIONAL);
       resultSet = this.searchService.query(searchParameters);

       //synchMemberNodesList = resultSet.getNodeRefs();
     
     }catch(Exception e) {
       
        if(logger.isDebugEnabled()) {logger.debug("Error finding Synch Member Nodes: " + e.getMessage());}
        throw e;
      
    }
     
    return resultSet;
   }
  
  
  
  /**
   * Get UnSynchMemberNodes
   * @return resultSet
   */
 public ResultSet getDirtyNodes(String ssdId) {
 
   //List<NodeRef> synchMemberNodesList = new ArrayList<NodeRef>();
   Boolean isSynched = false;
        
   final StringBuffer query = new StringBuffer("ASPECT:\"synch:synchMemberNodeAspect\" AND -ASPECT:\"cm:workingcopy\" AND =synch\\:synchSetDefID:\"" +
   ssdId + "\" AND =synch\\:isSynched:\"" +isSynched + "\"");
    
   ResultSet resultSet = null;
      
   try{
      if(logger.isDebugEnabled()) {logger.debug("Find UnSynched Member Nodes search query " + query.toString());}
      
      StoreRef storeRef = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");
  
      SearchParameters searchParameters = new SearchParameters();
      searchParameters.addStore(storeRef);
      searchParameters.setLanguage(SearchService.LANGUAGE_FTS_ALFRESCO);
      searchParameters.setQuery(query.toString());
      searchParameters.setQueryConsistency(QueryConsistency.TRANSACTIONAL);
      resultSet = this.searchService.query(searchParameters);

      //synchMemberNodesList = resultSet.getNodeRefs();
    
    }catch(Exception e) {
      
       if(logger.isDebugEnabled()) {logger.debug("Error finding Synch Member Nodes: " + e.getMessage());}
       throw e;
     
   }
    
   return resultSet;
  }

  
  
  public NodeRef getTargetSynchRootFolderNode(String ssdId) throws FileNotFoundException {
    

     
    final StringBuffer query = new StringBuffer("ASPECT:\"synch:synchTargetAspect\" AND =synch\\:targetSynchSetDefID:\"" +ssdId + "\"");
     
    ResultSet resultSet = null;
    NodeRef nodeRef = null;
       
    try{
       if(logger.isDebugEnabled()) {logger.debug("Find Synch Member Nodes search query " + query.toString());}
       
       StoreRef storeRef = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");
   
       SearchParameters searchParameters = new SearchParameters();
       searchParameters.addStore(storeRef);
       searchParameters.setLanguage(SearchService.LANGUAGE_FTS_ALFRESCO);
       searchParameters.setQuery(query.toString());
       
       resultSet = this.searchService.query(searchParameters);

       if (resultSet.length() == 1) {
         
         nodeRef = resultSet.getNodeRef(0);
         if(logger.isDebugEnabled()) {logger.debug("Target Synch Root Folder Found " + nodeRef);}
         
       }else{
         throw new FileNotFoundException("Unable to locate Target Synch Root Folder for ssid " + ssdId);
       }
     
     } catch(Exception e) {
       
        if(logger.isDebugEnabled()) {logger.debug("Unable to locate Target Synch Root Folder for ssid " + ssdId + e.getMessage());}
        throw new FileNotFoundException("Unable to locate Target Synch Root Folder for ssid " + ssdId + e);
      
    }
     
    return nodeRef;
   }
  //Create or Get SynchSetDefinitionNode Container
  /**
   * This method finds the SyncSet Definition Container NodeRef, creating one if it does not exist.
   * 
   * @return the syncset definition container
   */
  public NodeRef getOrCreateSynchSetDefinitionContainer()
  {

      QName container = QName.createQName("SeedSynchSetDefinitions", namespaceService);
      
      NodeRef systemSsdContainer = SystemNodeUtils.getSystemChildContainer(container, nodeService, repositoryHelper);
      
      if (systemSsdContainer == null)
      {   
          if(logger.isDebugEnabled()) {logger.debug("Creating SyncSetDefContainer");}
          systemSsdContainer = SystemNodeUtils.getOrCreateSystemChildContainer(container, nodeService, repositoryHelper).getFirst();
      }
      return systemSsdContainer;
  }
  
  /**
   * Create SynchSetDefinitionNode and return its nodeRef
   * 
   * @param synchSourceNodeRef
   * @return NodeRef
   */
  
  public NodeRef createSynchSetDefinitionNode(NodeRef synchSourceNodeRef){
    
    if(logger.isDebugEnabled()) {logger.debug("Entered createSynchSetDefinitionNode");}
    
    final String ssdId = GUID.generate();
    
    if(logger.isDebugEnabled()) {logger.debug("ssdId "  + ssdId);}
    NodeRef synchSetDefinitionContainer = this.getOrCreateSynchSetDefinitionContainer();
    //Get Properties from synchSourceNodeRef
    Map<QName, Serializable> synchSourceNodeProps = nodeService.getProperties(synchSourceNodeRef);
    String synchCreator = (String) synchSourceNodeProps.get(SeedSynchModel.PROP_CREATOR_USERNAME);
    Boolean includeSubfolders = (Boolean) synchSourceNodeProps.get(SeedSynchModel.PROP_INCLUDE_SUBFOLDERS);
    
    List<AssociationRef> synchSourceAssocs = nodeService.getTargetAssocs(synchSourceNodeRef,SeedSynchModel.ASSOC_TARGET_ROOT_FOLDER);
    
    //Need to Check is assocs not empty
    NodeRef ssdNodeRef = null;
    if(!synchSourceAssocs.isEmpty()){
    
      NodeRef targetRootFolderNodeRef = synchSourceAssocs.get(0).getTargetRef();
      String targetRootFolder = targetRootFolderNodeRef.toString();
      if(logger.isDebugEnabled()) {logger.debug("targetRootFolder "  + targetRootFolder);}
      
      Map<QName, Serializable> ssdProperties = new HashMap<QName, Serializable>();
      
      ssdProperties.put(SeedSynchModel.PROP_SYNCH_SET_ID, ssdId);
      ssdProperties.put(SeedSynchModel.PROP_SYNCH_SET_CREATOR, synchCreator);
      ssdProperties.put(SeedSynchModel.PROP_SYNCH_SET_INCLUDE_SUBFOLDERS, includeSubfolders);
      ssdProperties.put(SeedSynchModel.PROP_SYNCH_SET_TARGET_ROOT_FOLDER, targetRootFolder);
      ssdProperties.put(SeedSynchModel.PROP_SYNCH_SET_JOB_STATUS, "Complete");
      
      
      
      ChildAssociationRef newChildAssoc = nodeService.createNode(synchSetDefinitionContainer,
          ContentModel.ASSOC_CHILDREN, ContentModel.ASSOC_CHILDREN,
          SeedSynchModel.TYPE_SYNCH_SET_DEFINITION,
          ssdProperties);
      
      ssdNodeRef = newChildAssoc.getChildRef();
      
      //Create Assoc between SyncSourceNode and SSD node
      if(nodeService.exists(ssdNodeRef)){
        
        this.disableBehaviour(synchSourceNodeRef, ContentModel.ASPECT_AUDITABLE);
        nodeService.createAssociation(synchSourceNodeRef, ssdNodeRef, SeedSynchModel.ASSOC_SYNCH_SET_DEF_NODE);
        this.enableBehaviour(synchSourceNodeRef, ContentModel.ASPECT_AUDITABLE);
        //Remove TargetAssoc from SyncSource
        //nodeService.removeAssociation(synchSourceNodeRef, targetRootFolderNodeRef, SeedSynchModel.ASSOC_TARGET_ROOT_FOLDER);
        
        if(logger.isDebugEnabled()) {logger.debug("createSynchSetDefinitionNode: create ssdNode " + ssdNodeRef );}
        
      }else{
       
        throw new AlfrescoRuntimeException(
            "Could not Create SynchSetDefinitionNode against the Sync Source " + synchSourceNodeRef );
      }
    }else{
      if(logger.isDebugEnabled()) {logger.debug("synchSourceAssocs empty: Target Root Folder not available yet");}  // BON - whats going on here?
    }
    
    if(logger.isDebugEnabled()) {logger.debug("Exited createSynchSetDefinitionNode");}
    
    return ssdNodeRef;
  }
  
  
  /**
   * Copy SourceFolder to Target Folder, Users standard fileFolderService copy function
   * 
   * @param synchSourceNodeRef
   * @return NodeRef of the target root folder
   * @throws FileExistsException
   * @throws FileNotFoundException
   */
  
  public NodeRef createTargetRootFolder(NodeRef synchSourceNodeRef, NodeRef ssdNodeRef) throws FileExistsException, FileNotFoundException{
    
    
      if(logger.isDebugEnabled()) {logger.debug("Entered createTargetRootFolder...."  );}
      
      
      String ssdId = (String)nodeService.getProperty(ssdNodeRef, SeedSynchModel.PROP_SYNCH_SET_ID);
      
      NodeRef targetNodeRef = null;
      String targetParentNodeRefString = (String) nodeService.getProperty(ssdNodeRef, SeedSynchModel.PROP_SYNCH_SET_TARGET_ROOT_FOLDER);
      NodeRef targetParentNodeRef = new NodeRef(targetParentNodeRefString);
      targetNodeRef = this.createTargetFolderFromSource(synchSourceNodeRef, targetParentNodeRef, ssdId);   
      
      //Add synchTargetAspect
      if(!nodeService.hasAspect(targetNodeRef, SeedSynchModel.ASPECT_SYNCH_TARGET)){  
      
          //Get current date
          Calendar cal = Calendar.getInstance();
          Date synchDate = cal.getTime();
          
          
          
          Map<QName, Serializable> targetRootNodeProps = new HashMap<QName, Serializable>();
          targetRootNodeProps.put(SeedSynchModel.PROP_TARGET_SYNCH_DATE,synchDate);
          targetRootNodeProps.put(SeedSynchModel.PROP_TARGET_SYNCH_DEF_NODE_ID, ssdId);
          nodeService.addAspect(targetNodeRef, SeedSynchModel.ASPECT_SYNCH_TARGET, targetRootNodeProps);
          
          // create an assiciation back to the source root folder
       // set the memberNodeDetails on the source
          nodeService.createAssociation(targetNodeRef, synchSourceNodeRef, SeedSynchModel.ASSOC_SOURCE_ROOT_FOLDER);
        
      }
      
      if(logger.isDebugEnabled()) {logger.debug("Exited createTargetRootFolder...."  );}
      
      return targetNodeRef;
    
  }
  
  
  /**
   * Set the node up with values for Source Membership aspect.  If targetRef not supplied then is not synced yet.
   * 
   * @param sourceNodeRef
   * @param targetNodeRef
   * @param ssdId
   * @param isSynced
   */
  public void addMemberDetailsProperties(NodeRef sourceNodeRef, NodeRef targetNodeRef, String ssdId) {
    
    if(logger.isDebugEnabled()) {logger.debug("Entered addMemberDetailsProperties for: " + sourceNodeRef  );}
    
    if(sourceNodeRef != null){
      
        try{
          
          
          if(ssdId == null){
            
            ssdId = this.getSynSetDefID(sourceNodeRef);
          }
          
          
          
          // if no member node aspect then set a new one
          
          // if aspect not set yet then this needs a new aspect
          if(!nodeService.hasAspect(sourceNodeRef, SeedSynchModel.ASPECT_SYNCH_MEMBER_NODE)){
            
            Map<QName, Serializable> memberNodeProps = new HashMap<QName, Serializable>();
            memberNodeProps.put(SeedSynchModel.PROP_SYNCH_DEF_NODE_ID, ssdId);
         // put referenced target node in file
            if(targetNodeRef != null){
                memberNodeProps.put(SeedSynchModel.PROP_SYNCED_TARGET_REF, targetNodeRef);
                memberNodeProps.put(SeedSynchModel.PROP_IS_SYNCHED, true);
            }else{
                memberNodeProps.put(SeedSynchModel.PROP_IS_SYNCHED, false);
            }
            
            nodeService.addAspect(sourceNodeRef, SeedSynchModel.ASPECT_SYNCH_MEMBER_NODE, memberNodeProps);
                
            if(logger.isDebugEnabled()) {logger.debug("addMemberDetailsProperties: Added aspect " + SeedSynchModel.ASPECT_SYNCH_MEMBER_NODE.toPrefixString()  );}
          }
          else
          {
            // update the aspect with the new details
            this.nodeService.setProperty(sourceNodeRef, SeedSynchModel.PROP_SYNCH_DEF_NODE_ID, ssdId);
            this.nodeService.setProperty(sourceNodeRef, SeedSynchModel.PROP_SYNCED_TARGET_REF, targetNodeRef);
            this.nodeService.setProperty(sourceNodeRef, SeedSynchModel.PROP_IS_SYNCHED, true);
            
            if(logger.isDebugEnabled()) {logger.debug("addMemberDetailsProperties: Already has aspect " + SeedSynchModel.ASPECT_SYNCH_MEMBER_NODE.toPrefixString()  );}
          }
        
        }finally{
          
          
          if(logger.isDebugEnabled()) {logger.debug("Exited addMemberDetailsProperties...."  );}
        }
    }
    
  }  
  
  /**
   * Create a replica source Folder in the target
   * 
   * @param sourceNodeRef
   * @return targetNodeRef
   * 
   * @throws FileExistsException
   * @throws FileNotFoundException
   */
  
  public NodeRef createTargetFolderFromSource(NodeRef sourceNodeRef, NodeRef targetNodeRef, String ssdId) throws FileExistsException, FileNotFoundException{
    
    
    if(logger.isDebugEnabled()) {logger.debug("Entered createTargetFolderFromSource...."  );}
    
    String srcFolderName = (String)nodeService.getProperty(sourceNodeRef, ContentModel.PROP_NAME);
    FileInfo fi = fileFolderService.create(targetNodeRef, srcFolderName , ContentModel.TYPE_FOLDER);
    
    // update the source file with the nodeRef for the target
    this.nodeService.setProperty(sourceNodeRef, SeedSynchModel.PROP_SYNCED_TARGET_REF, fi.getNodeRef().toString());
    
    
    // add the member details aspect to source
 // set the nodeRef into the source
    addMemberDetailsProperties(sourceNodeRef, fi.getNodeRef(), ssdId);  // set the issynced to true because we are creating the
    
    // sync the properties from the source to the target for the top level folder
    this.syncPropertyChange(sourceNodeRef);
    
    if(logger.isDebugEnabled()) {logger.debug("createTargetFolderFromSource:  Created targetFolder: " + fi.getName()  );}
            
     return fi.getNodeRef();
  
  }
  


  /**
   * Create a replica source Folder in the target
   * 
   * @param sourceNodeRef
   * @return targetNodeRef
   * 
   * @throws FileExistsException
   * @throws FileNotFoundException
   */
  
  public NodeRef createTargetFileFromSource(NodeRef sourceNodeRef, NodeRef targetNodeRef, String ssdId) throws FileExistsException, FileNotFoundException{
    
    
      if(logger.isDebugEnabled()) {logger.debug("Entered createTargetFileFromSource entered"  );}
      
      NodeRef createdNodeRef = null;
      
      List<QName> setPropertyQNameList = this.toPropertyNames(propertiesToTrack);
      String srcName = (String)nodeService.getProperty(sourceNodeRef, ContentModel.PROP_NAME);
      
      
      // Create Node metadata
      QName associationType = ContentModel.ASSOC_CONTAINS;
      
      
      // Set the properties
      Map<QName, Serializable> nodeProperties = new HashMap<QName, Serializable>();
      nodeProperties.put(ContentModel.PROP_NAME, srcName);
      for(int i = 0; i < setPropertyQNameList.size()  ; i++){ 
        nodeProperties.put(setPropertyQNameList.get(i), nodeService.getProperty(sourceNodeRef, setPropertyQNameList.get(i)));
      }
      

      // create the node
      QName associationQName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI,
          QName.createValidLocalName(srcName));
    
      ChildAssociationRef parentChildAssocRef = nodeService.createNode(
          targetNodeRef, ContentModel.ASSOC_CONTAINS, associationQName, ContentModel.TYPE_CONTENT, nodeProperties);
      
      createdNodeRef = parentChildAssocRef.getChildRef();
      
      this.copyContent(sourceNodeRef, createdNodeRef);
      
      
      // set the nodeRef into the source
      addMemberDetailsProperties(sourceNodeRef, createdNodeRef, ssdId);  // set the issynced to true because we are creating the
    
    
    if(logger.isDebugEnabled()) {logger.debug("createTargetFileFromSource:  Created target file: " + createdNodeRef  );}
            
     return createdNodeRef;
  
  }
  
  
  /**
   * Copy Content from Source to Target file
   *
   * @param sourceNodeRef
   * @param targetRef
   */
  private void copyContent( NodeRef sourceNodeRef, NodeRef targetRef) {
    
    logger.debug("Entered copyContent method" ); 
     
    ContentData contentData = (ContentData) nodeService.getProperty(sourceNodeRef, ContentModel.PROP_CONTENT);
    if(contentData != null){
     
        ContentWriter writer = contentService.getWriter(targetRef, ContentModel.PROP_CONTENT, true);
        
     // Reading the data content of a NodeRef (binary)
        ContentReader reader = contentService.getReader(sourceNodeRef, ContentModel.PROP_CONTENT);
        InputStream originalInputStream = reader.getContentInputStream();
        
        String originalMimeType = contentData.getMimetype();
        String encoding = contentData.getEncoding();
        Locale locale = contentData.getLocale();
        logger.debug("Mimetype determined as " + originalMimeType);
        writer.setMimetype(originalMimeType);
        writer.setEncoding(encoding);
        writer.setLocale(locale);
        
     
        // add the content regardless
        if (logger.isDebugEnabled()) logger.debug("Writing content to node");
        writer.putContent(originalInputStream);
        
        
    }
        
    logger.debug("Exited copyContent method"); 
  }

  
  
  
  
  /**
   * Recursive Method to get Nodes for Synch
   * 
   * @param parentNodeRef
   * @param includeSubFolders
   * @param childNodeRefs
   * @param skipSyncedChildren
   */
//  public void getSynchMemberNodes(NodeRef parentNodeRef, boolean includeSubFolders, List<NodeRef> childNodeRefs)  //BON Todo:  Clean up if statements
//  {
//      if(logger.isDebugEnabled()) {logger.debug("Entered getSynchMemberNode...."  );}
//      
//      List<FileInfo> children = new ArrayList<>();
//      if (includeSubFolders)
//      {
//          List<ChildAssociationRef> allChildren = nodeService.getChildAssocs(parentNodeRef);
//
//          for (ChildAssociationRef assocRef : allChildren)
//          {
//              children.add(fileFolderService.getFileInfo(assocRef.getChildRef()));
//          }
//      }
//      else
//      {
//          children = fileFolderService.listFiles(parentNodeRef);
//      }
//      
//      // for each node
//      for (FileInfo child : children)
//      {
//          NodeRef childNodeRef = child.getNodeRef();
//          if (isSyncSetMemberNode(childNodeRef))
//          {
//              if (logger.isInfoEnabled()) {logger.info("getChildren: skip "+(child.isFolder() ? "folder" : "file")+" - already sync'ed: "+childNodeRef);}
//          }
//          else if (nodeService.hasAspect(childNodeRef, ContentModel.ASPECT_WORKING_COPY))
//          {
//              if (logger.isTraceEnabled()){ logger.trace("getChildren: skip working copy: "+childNodeRef); }
//              // Do Noting
//          }
//          else
//          {
//              if (logger.isTraceEnabled()){ logger.trace("getChildren: add "+(child.isFolder() ? "folder" : "file")+" to list: "+childNodeRef);}
//              childNodeRefs.add(childNodeRef);
//          }
//          
//          if (includeSubFolders && child.isFolder())
//          {
//              // recurse
//            getSynchMemberNodes(childNodeRef, includeSubFolders, childNodeRefs, skipSyncedChildren);
//          }
//      }
//      if(logger.isDebugEnabled()) {logger.debug("Exited getSynchMemberNode...."  );}
//  }
  
  
  public NodeRef findSynchTargetNode(NodeRef synchTargetRootFolder, String sourceParentName, String sourceMemberName){
    if(logger.isDebugEnabled()) {logger.debug("Entered findSynchTargetNode...."  );}
    //List<FileInfo> children = fileFolderService.listFiles(synchTargetRootFolder);
    List<FileInfo> children = fileFolderService.list(synchTargetRootFolder);
    NodeRef targetNode = null;
    //found = isFound;
     for (FileInfo child : children)
      {
        if(targetNode == null){
          NodeRef childNodeRef = child.getNodeRef();
          List<ChildAssociationRef> parentAssocs = nodeService.getParentAssocs(childNodeRef);
          if(!parentAssocs.isEmpty()){
            NodeRef parentRef = parentAssocs.get(0).getParentRef();
            String targetParentName = (String) nodeService.getProperty(parentRef, ContentModel.PROP_NAME);
            String targetNodeName = (String) nodeService.getProperty(childNodeRef, ContentModel.PROP_NAME);
            if(logger.isDebugEnabled()) {logger.debug("targetParentName: " +targetParentName + "- sourceParentName:" + sourceParentName);}
            if(logger.isDebugEnabled()) {logger.debug("targetNodeName: " +targetNodeName + "- sourceMemberName:" + sourceMemberName);}
            if(targetParentName.matches(sourceParentName) && targetNodeName.matches(sourceMemberName)){
                if(logger.isDebugEnabled()) {logger.debug("Match Found...."  );}
                targetNode = childNodeRef;
                found = true;
                //break;
                return targetNode;
            }else {
              if(logger.isDebugEnabled()) {logger.debug("Match NOT Found check inside Folder...."  );}
              if (child.isFolder() && targetNode == null){
              //recurse
                if(logger.isDebugEnabled()) {logger.debug("Recursing"  );}
                targetNode = findSynchTargetNode(childNodeRef,sourceParentName,sourceMemberName);
              }
            }
          }
        }
    }
    if(logger.isDebugEnabled()) {logger.debug("SynchTargetNodeRef " +  targetNode);}
    if(logger.isDebugEnabled()) {logger.debug("Exited findSynchTargetNode...."  );}
    return targetNode;
  }
  
  
  /**
   * 
   * @param nodeRef
   * @return
   */
  public boolean isSyncSetMemberNode(NodeRef nodeRef)
  {
      return nodeService.hasAspect(nodeRef, SeedSynchModel.ASPECT_SYNCH_MEMBER_NODE);
  }
  
  
  /**
   * Remove SyncSource Aspect and Assoc from SyncTargetNode
   * 
   * @param synchTargetRootFolder
   */
  public void sanitiseSynchTargetRootFolder(NodeRef synchTargetRootFolder, NodeRef synchSourceNodeRef, String ssdId){
    
    if(logger.isDebugEnabled()) {logger.debug("Entered sanitiseSynchTargetRootFolder...."  );}
    if(nodeService.exists(synchTargetRootFolder)){

      if(nodeService.hasAspect(synchTargetRootFolder, SeedSynchModel.ASPECT_SYNCH_SOURCE)){
        List<AssociationRef> synchTargetAssocs = nodeService.getTargetAssocs(synchTargetRootFolder,SeedSynchModel.ASSOC_TARGET_ROOT_FOLDER);
        if(!synchTargetAssocs.isEmpty()){
          nodeService.removeAssociation(synchTargetRootFolder, synchTargetAssocs.get(0).getTargetRef(), SeedSynchModel.ASSOC_TARGET_ROOT_FOLDER);
          
        }
        nodeService.removeAspect(synchTargetRootFolder, SeedSynchModel.ASPECT_SYNCH_SOURCE);
      } 
    }
    if(logger.isDebugEnabled()) {logger.debug("Exited sanitiseSynchTargetRootFolder...."  );}
  }
  
  /**
   * Set SyncTargetAspect against target node
   * 
   * @param synchTargetRootFolder
   */
  public void markAsSynchTargetRootFolder(NodeRef synchTargetRootFolder, NodeRef synchSourceNodeRef, String ssdId){
    
    if(logger.isDebugEnabled()) {logger.debug("Entered markAsSynchTargetRootFolder...."  );}
    if(nodeService.exists(synchTargetRootFolder)){
    
      //Add synchTargetAspect
      if(!nodeService.hasAspect(synchTargetRootFolder, SeedSynchModel.ASPECT_SYNCH_TARGET)){   //BON Why is this being done here... Should this not be done as part of the initial Copy
      
        //Get current date
        Calendar cal = Calendar.getInstance();
        Date synchDate = cal.getTime();
        Map<QName, Serializable> targetRootNodeProps = new HashMap<QName, Serializable>();
        targetRootNodeProps.put(SeedSynchModel.PROP_TARGET_SYNCH_DATE,synchDate);
        targetRootNodeProps.put(SeedSynchModel.PROP_TARGET_SYNCH_DEF_NODE_ID,ssdId);
        nodeService.addAspect(synchTargetRootFolder, SeedSynchModel.ASPECT_SYNCH_TARGET, targetRootNodeProps);
        nodeService.createAssociation(synchTargetRootFolder, synchSourceNodeRef, SeedSynchModel.ASSOC_SOURCE_ROOT_FOLDER);
      
      }
    }
    if(logger.isDebugEnabled()) {logger.debug("Exited markAsSynchTargetRootFolder...."  );}
  }
  
  
  
  /**
   * Removee Synch Metadata and delete SSD node
   * @param synchSourceNodeRef
   */
  public void removeSynch(final NodeRef synchSourceNodeRef){
    

    try{
      this.disableBehaviour(synchSourceNodeRef, ContentModel.ASPECT_AUDITABLE);
    
        //this.disableSynchBehaviours();
        //Get SynchSetDefID, Search all member nodes based on SynchSetID and Remove synchMemberNodeAspect
        Map<QName, Serializable> synchSourceProps = nodeService.getProperties(synchSourceNodeRef);
        String ssdID = null;
        if(nodeService.hasAspect(synchSourceNodeRef, SeedSynchModel.ASPECT_SYNCH_MEMBER_NODE)){
          ssdID = (String) synchSourceProps.get(SeedSynchModel.PROP_SYNCH_DEF_NODE_ID);
          ResultSet synchMemberNodes = this.getAllSynchMemberNodes(ssdID);
          if(synchMemberNodes.length() > 0){
            for (NodeRef synchMemberNode : synchMemberNodes.getNodeRefs()){
              if(nodeService.hasAspect(synchMemberNode, SeedSynchModel.ASPECT_SYNCH_MEMBER_NODE)){
                this.disableBehaviour(synchMemberNode, ContentModel.ASPECT_AUDITABLE);
                nodeService.removeAspect(synchMemberNode, SeedSynchModel.ASPECT_SYNCH_MEMBER_NODE);
                this.enableBehaviour(synchMemberNode, ContentModel.ASPECT_AUDITABLE);
              }
            }

          }
        }
        //finally remove SynchSourceMetadata
        RunAsWork<Void> work = new RunAsWork<Void>() {
          public Void doWork() throws Exception {
            try {
              if(logger.isDebugEnabled()) logger.debug("Removing SynchSourceMetadata");  
              disableAllBehaviours();
              removeSynchSourceMetadata(synchSourceNodeRef);
              enableAllBehaviours();
            } catch (Exception e) {
              String msg = String.format("Could not remove SynchSourceMetadata %s", e.getMessage());
              logger.debug(msg);
              e.printStackTrace();
            }
            return null;
          }
        };
        String user = AuthenticationUtil.getAdminUserName();
        AuthenticationUtil.runAs(work, user);
       
      //Get Synch Target Node and Remove metadata
        if(ssdID != null){
          try {
            NodeRef synchTargetNodeRef = this.getTargetSynchRootFolderNode(ssdID);
            if(synchTargetNodeRef != null && nodeService.exists(synchTargetNodeRef)){
              this.removeSynchTargetMetadata(synchTargetNodeRef);
            }
          } catch (FileNotFoundException e) {
            if(logger.isDebugEnabled()) {logger.debug("SynchTargetRootFolder not found...."  );}
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        }
    }finally{
      this.enableBehaviour(synchSourceNodeRef, ContentModel.ASPECT_AUDITABLE);
    }
    
  }
  
  
  
  
  
  
  
  /**
   * Process each node
   * 
   * @param syncNodes
   * @param synchSourceNodeRef
   * @param targetRootFolderNodeString
   * @throws FileExistsException
   * @throws FileNotFoundException
   */
  public void syncNodes(ResultSet syncNodes) throws FileExistsException, FileNotFoundException{
    
//    NodeRef targetRootFolderNodeRef = new NodeRef(targetRootFolderNodeString);
//    String sourceFolderName = (String) nodeService.getProperty(synchSourceNodeRef, ContentModel.PROP_NAME);
//    NodeRef targetFolderNodeRef = nodeService.getChildByName(targetRootFolderNodeRef, ContentModel.ASSOC_CONTAINS, sourceFolderName);
//    
      
      if(syncNodes.length() > 0){
        
          
          for (NodeRef sourceNodeRef : syncNodes.getNodeRefs()){
            
            boolean syncLockOwner = false;
                //get syncNode Parent Name and Check is it exist in target   
              try{
                  this.disableSynchBehaviours();                 
                  this.disableBehaviour(sourceNodeRef, ContentModel.ASPECT_AUDITABLE);
                  
                  if(this.isNodeLocked(sourceNodeRef)){
                    
                    // do no process this node          
                    continue;
                  }else{
                    lockService.lock(sourceNodeRef, LockType.NODE_LOCK);
                    syncLockOwner = true;
                  }
                  
              
                  
                  NodeRef targetParentRef = null;
                  
                  if(!nodeService.hasAspect(sourceNodeRef, SeedSynchModel.ASPECT_SYNCH_SOURCE)){
                    NodeRef sourceParentRef = this.nodeService.getPrimaryParent(sourceNodeRef).getParentRef();
                    String targetParentRefString = (String)this.nodeService.getProperty(sourceParentRef, SeedSynchModel.PROP_SYNCED_TARGET_REF);
                    targetParentRef = new NodeRef(targetParentRefString);
                  }
                  Map<QName, Serializable> nodeProps = nodeService.getProperties(sourceNodeRef);
                  Boolean contentChanged = (Boolean)nodeProps.get(SeedSynchModel.PROP_HAS_CONTENT_CHANGED);
                  Boolean propertyChanged = (Boolean)nodeProps.get(SeedSynchModel.PROP_HAS_PROPERTIES_CHANGED);
                  
                  if(logger.isDebugEnabled()) {logger.debug("syncNodes: contentChanged:  " + contentChanged + ", propertyChanged: " + propertyChanged  );}
                  
                  boolean newNode = (!contentChanged.booleanValue() && !propertyChanged.booleanValue());
                  
                  // Process New Nodes
                  if(newNode){
                    
                    if(logger.isDebugEnabled()) {logger.debug("syncNodes: sync new node."  );}
                    
                      if(fileFolderService.getFileInfo(sourceNodeRef).isFolder()){
                        // create folder
                        this.createTargetFolderFromSource(sourceNodeRef, targetParentRef, null);
                        
                      }
                      else{
                        NodeRef createdTargetFile = this.createTargetFileFromSource(sourceNodeRef, targetParentRef, null);
                        //KT apply versionable aspect
                        if(nodeService.exists(createdTargetFile)){
                          nodeService.addAspect(createdTargetFile, ContentModel.ASPECT_VERSIONABLE, null);
                        }
                        
                      }
                  }
                  
                  // if existing node and poperty changed
                  if(propertyChanged){
                    
                    if(logger.isDebugEnabled()) {logger.debug("syncNodes: sync property change."  );}
                    syncPropertyChange(sourceNodeRef);
                  }
                  
                  // if existing node and content changed
                  if(contentChanged){
                    
                    if(logger.isDebugEnabled()) {logger.debug("syncNodes: sync content change."  );}
                    
                    syncContentChange(sourceNodeRef);
                  }
                  
                  this.markAsSynced(sourceNodeRef);
                  
            }finally{
              
              
              
              this.unlockNode(sourceNodeRef, syncLockOwner);
              
              this.enableBehaviour(sourceNodeRef, ContentModel.ASPECT_AUDITABLE);
              this.enableSynchBehaviours();
            
            }
          } 
      }
  }
      
    /**
     * Update the target node with the new content and increase target version by 1 major version.
     * 
     * @param nodeRef:  Source node ref
     */
    private void syncContentChange(NodeRef nodeRef) {
          
            
            if(logger.isDebugEnabled()) {logger.debug("syncContentChange entered");}
            
            // get the nodeRef of the target
            String nodeRefStr = (String)nodeService.getProperty(nodeRef, SeedSynchModel.PROP_SYNCED_TARGET_REF);
            NodeRef targetRef = new NodeRef(nodeRefStr);
            // 
            
          //create new version and add content
            Map<String, Serializable> versionProperties = new HashMap<String, Serializable>();
            versionProperties.put(VersionModel.PROP_VERSION_TYPE, VersionType.MAJOR);
            versionProperties.put(VersionModel.PROP_DESCRIPTION, "Sync Update of Content");
            Version versionNode = versionService.createVersion(targetRef, versionProperties);
            
            if(logger.isDebugEnabled()) {logger.debug("syncContentChange Version updated to " + versionNode.getVersionLabel());}
            
            // copy content to new node
            this.copyContent(nodeRef, targetRef);
            
            if(logger.isDebugEnabled()) {logger.debug("syncContentChange exited");}
        
             
    }

      /**
       * Update target properties as defined in bean xml 
       * @param nodeRef
       */
      private void syncPropertyChange(NodeRef nodeRef) {
    
        if(logger.isDebugEnabled()) {logger.debug("syncPropertyChange entered");}
        
        // get the nodeRef of the target
        String nodeRefStr = (String)nodeService.getProperty(nodeRef, SeedSynchModel.PROP_SYNCED_TARGET_REF);
        NodeRef targetRef = new NodeRef(nodeRefStr);
        // 
        
        List<QName> propertiesList = toPropertyNames(this.propertiesToTrack);
        for (QName propertyQName : propertiesList){
          
          // updating property
          //QName propQName = QName.createQName(propertyName);
          Serializable sourceVal = nodeService.getProperty(nodeRef, propertyQName);
                 
          
          nodeService.setProperty(targetRef, propertyQName, sourceVal);
          
          if(logger.isDebugEnabled()) {logger.debug("syncPropertyChange: property set: " + propertyQName + " to " + sourceVal);}
          
        }
        
        if(logger.isDebugEnabled()) {logger.debug("syncPropertyChange exited");}
        
  }
      
    
    
    /**
     * Updates SynchMemberNode properties based of type of change
     * @param synchMemberNode
     * @param mode
     */
    
    private void markAsSynced(NodeRef nodeRef){
      
    
      try{
      
          nodeService.setProperty(nodeRef, SeedSynchModel.PROP_IS_SYNCHED, true);
          nodeService.setProperty(nodeRef, SeedSynchModel.PROP_HAS_PROPERTIES_CHANGED, false);
          nodeService.setProperty(nodeRef, SeedSynchModel.PROP_HAS_CONTENT_CHANGED, false);
      
      }finally{
        
      }

    }
    
  
  
  
  /**
   * Recursive Method to sanitise SynchedTargetNodes
   * @param targetSynchMemberNode
   */
  public void sanitiseSynchedTargetNodesRecursively(NodeRef targetSynchMemberNode){
    
    if(nodeService.hasAspect(targetSynchMemberNode, SeedSynchModel.ASPECT_SYNCH_MEMBER_NODE))
    {
      nodeService.removeAspect(targetSynchMemberNode, SeedSynchModel.ASPECT_SYNCH_MEMBER_NODE);
    }
    
    List<FileInfo> children = fileFolderService.list(targetSynchMemberNode);
    if(!children.isEmpty()){
      for (FileInfo child : children){
           NodeRef childNodeRef = child.getNodeRef();
           if(nodeService.exists(childNodeRef) && isSyncSetMemberNode(childNodeRef)){
             //Remove SyncMemberNode Aspect from targetSynchNode
             if(logger.isDebugEnabled()) {logger.debug("Removing Synch Member Node Aspect from target node.."  + childNodeRef);}
             
             if(nodeService.hasAspect(childNodeRef, SeedSynchModel.ASPECT_SYNCH_MEMBER_NODE)){
               nodeService.removeAspect(childNodeRef, SeedSynchModel.ASPECT_SYNCH_MEMBER_NODE);
             }
           }
           
           if(child.isFolder()){
             sanitiseSynchedTargetNodesRecursively(childNodeRef);
           }
      
      }
     }else{
        if(logger.isDebugEnabled()) {logger.debug("No children for node.."  + targetSynchMemberNode);}
      }
     
    
  }
  
    
  
  /**
   * removeSynchTargetMetadata - BON
   * @param synchTargetNodeRef
   */
  
  public void removeSynchTargetMetadata(NodeRef synchTargetNodeRef){
    if(nodeService.hasAspect(synchTargetNodeRef, SeedSynchModel.ASPECT_SYNCH_TARGET)){
      //Removed SynchSetDefNodeAssoc
        List<AssociationRef> synchTargetAssocs = nodeService.getTargetAssocs(synchTargetNodeRef,SeedSynchModel.ASSOC_SOURCE_ROOT_FOLDER);
        if(!synchTargetAssocs.isEmpty()){
          NodeRef synchSourceNodeRef = synchTargetAssocs.get(0).getTargetRef();
          nodeService.removeAssociation(synchTargetNodeRef,synchSourceNodeRef, SeedSynchModel.ASSOC_SOURCE_ROOT_FOLDER);
        }
      //Finally Aspects on TargetFolder
        nodeService.removeAspect(synchTargetNodeRef, SeedSynchModel.ASPECT_SYNCH_TARGET);
      }
  }

  /**
   * removeSynchSourceMetadata
   * @param synchSourceNodeRef
   */
  
  public void removeSynchSourceMetadata(NodeRef synchSourceNodeRef){
    
    try{
      
      
      if(nodeService.hasAspect(synchSourceNodeRef, SeedSynchModel.ASPECT_SYNCH_SOURCE)){
        
        //Removed SynchSetDefNodeAssoc
          List<AssociationRef> synchSourceAssocs = nodeService.getTargetAssocs(synchSourceNodeRef, SeedSynchModel.ASSOC_SYNCH_SET_DEF_NODE);
          if(!synchSourceAssocs.isEmpty()){
            NodeRef ssdNodeRef = synchSourceAssocs.get(0).getTargetRef();
            nodeService.removeAssociation(synchSourceNodeRef,ssdNodeRef , SeedSynchModel.ASSOC_SYNCH_SET_DEF_NODE);
            //Delete SynchSetDefNode
            nodeService.deleteNode(ssdNodeRef);
          }
            
          // BON
          //Finally Aspects on SourceFolder
          nodeService.removeAspect(synchSourceNodeRef, SeedSynchModel.ASPECT_SYNCH_SOURCE);
          
          //remove the synch:sourceRootFolderAssoc 
          
        }
        //Remove FailedSynchAspect is applied
        if(nodeService.hasAspect(synchSourceNodeRef, SeedSynchModel.ASPECT_SYNCH_FAILED)){
          nodeService.removeAspect(synchSourceNodeRef, SeedSynchModel.ASPECT_SYNCH_FAILED);
        }
        if(nodeService.hasAspect(synchSourceNodeRef, SeedSynchModel.ASPECT_SYNCH_MEMBER_NODE)){
          nodeService.removeAspect(synchSourceNodeRef, SeedSynchModel.ASPECT_SYNCH_MEMBER_NODE);
        }
    }finally{
      
    }
  }
  
  /**
   * 
   * @param nodeRef
   * @return
   */
  public NodeRef getSynchedParent(NodeRef nodeRef){
    if(logger.isDebugEnabled()) {logger.debug("getSynchedParent for node.."  + nodeRef);}
    NodeRef synchedParent = null;
    List<ChildAssociationRef> parentAssocs = nodeService.getParentAssocs(nodeRef);
    if(!parentAssocs.isEmpty()){
      NodeRef synchMemberNodeParent = parentAssocs.get(0).getParentRef();
      Boolean isSynched = (Boolean) nodeService.getProperty(synchMemberNodeParent, SeedSynchModel.PROP_IS_SYNCHED);
      if(isSynched != null && !isSynched){
        synchedParent = getSynchedParent(synchMemberNodeParent);
      }else{
        synchedParent = synchMemberNodeParent;
      }
    }
    if(logger.isDebugEnabled()) {logger.debug("Synched Source Parent node.."  + synchedParent);}
    return synchedParent;
  }
  
  /**
   * Apply Synch Failed Aspect and set its properties
   * @param synchSourceNodeRef
   * @param errorDetails
   */
  public void applySynchFailedMetadata(NodeRef ssdNodeRef, NodeRef synchSourceNodeRef, String errorDetails){

    try{
      
      this.disableBehaviour(synchSourceNodeRef, ContentModel.ASPECT_AUDITABLE);
      this.disableBehaviour(synchSourceNodeRef, SeedSynchModel.ASPECT_SYNCH_MEMBER_NODE);
      Calendar cal = Calendar.getInstance();
      Date synchErrorDate = cal.getTime();
      String synchErrorDetails = "Synch Failed for node: " + synchSourceNodeRef + " due to error: " + errorDetails;
      Map<QName, Serializable> sourceNodeErrorProps = new HashMap<QName, Serializable>();
      sourceNodeErrorProps.put(SeedSynchModel.PROP_SYNCH_ERROR_TIME,synchErrorDate);
      sourceNodeErrorProps.put(SeedSynchModel.PROP_SYNCH_ERROR_DETAILS,synchErrorDetails);
        
      nodeService.addAspect(synchSourceNodeRef, SeedSynchModel.ASPECT_SYNCH_FAILED, sourceNodeErrorProps);
      if(ssdNodeRef != null && nodeService.exists(ssdNodeRef)){
        
        nodeService.setProperty(ssdNodeRef, SeedSynchModel.PROP_SYNCH_SET_JOB_STATUS, "Failed");
      }

    }finally{
    
      this.enableBehaviour(synchSourceNodeRef, ContentModel.ASPECT_AUDITABLE);
      this.enableBehaviour(synchSourceNodeRef, SeedSynchModel.ASPECT_SYNCH_MEMBER_NODE);
    }
  }
  
  public void disableSynchBehaviours()
  {
      behaviourFilter.disableBehaviour(SeedSynchModel.ASPECT_SYNCH_MEMBER_NODE);
  }
  
  public void enableSynchBehaviours()
  {
      behaviourFilter.enableBehaviour(SeedSynchModel.ASPECT_SYNCH_MEMBER_NODE);

  }
  public void disableBehaviour(NodeRef nodeRef, QName aspect)
  {
      behaviourFilter.disableBehaviour(nodeRef, aspect);
  }
  
  public void enableBehaviour(NodeRef nodeRef, QName aspect)
  {
      behaviourFilter.enableBehaviour(nodeRef, aspect);

  }
  public void disableNodeBehaviours(NodeRef nodeRef)
  {
      behaviourFilter.disableBehaviour(nodeRef);
  }
  
  public void enableNodeBehaviours(NodeRef nodeRef)
  {
      behaviourFilter.enableBehaviour(nodeRef);
  }
  
  
  
  public void disableAllBehaviours() {
    RunAsWork<Void> work = new RunAsWork<Void>() {
      public Void doWork() throws Exception {
        behaviourFilter.disableBehaviour();
        return null;
      }
    };
    String user = AuthenticationUtil.getSystemUserName();
    AuthenticationUtil.runAs(work, user);
  }
  
  public void enableAllBehaviours() {
    RunAsWork<Void> work = new RunAsWork<Void>() {
      public Void doWork() throws Exception {
        behaviourFilter.enableBehaviour();
        return null;
      }
    };
    String user = AuthenticationUtil.getSystemUserName();
    AuthenticationUtil.runAs(work, user);
  }
  
  /**
   * This method takes the injected property names or aspect name wildcards, validating that they are recognised property QNames.
   * @param propIdentifiers a list of property identifiers where each is either a property name such as "cm:name" or an aspect name with wildcard: "cm:titled.*"
   * @throws AlfrescoRuntimeException if any name is invalid.
   */
  public List<QName> toPropertyNames(List<String> propIdentifiers)
  {
      final List<QName> result = new ArrayList<>();

      if (propIdentifiers != null)
      {
          final Pattern propertyWildcardRegEx = Pattern.compile("(.+)\\.\\*");

          for (final String propertyIdentifier : propIdentifiers)
          {
              //if (logger.isDebugEnabled()){logger.debug("Setting property to track: '" + propertyIdentifier + "'");}

              if (propertyIdentifier != null && !propertyIdentifier.trim().isEmpty())
              {
                  final Matcher m = propertyWildcardRegEx.matcher(propertyIdentifier);
                  if (m.matches())
                  {
                      // Then we must have the name of an aspect
                      final String aspectName = m.group(1);

                      final QName aspectQName = QName.createQName(aspectName, namespaceService);
                      AspectDefinition aspectDef = dictionaryService.getAspect(aspectQName);
                      if (aspectDef != null)
                      {
                          Map<QName, PropertyDefinition> propDefs = aspectDef.getProperties();
                          for (QName qname : propDefs.keySet())
                          {
                              // Note that the above propDefs will include all inherited properties.
                              //if(logger.isDebugEnabled()){logger.debug("tracking property : " + qname);}
                              result.add(qname);
                          }
                      }
                      else
                      {
                          throw new AlfrescoRuntimeException("Unrecognised aspect name: " + aspectName);
                      }
                  }
                  else
                  {
                      // else we must have the name of a property.
                      final QName propQName = QName.createQName(propertyIdentifier, namespaceService);
                      final PropertyDefinition propertyDefn = dictionaryService.getProperty(propQName);
                      if (propertyDefn == null)
                      {
                          throw new AlfrescoRuntimeException("Unrecognised property name: " + propertyIdentifier);
                      }
                      //if(logger.isDebugEnabled()){logger.debug("tracking property : " + propQName);}
                      result.add(propQName);
                  }
              }
          }
      }
      return result;
  }
  
  public Map<QName, Serializable> filterIrrelevantProperties(List<QName> qnameList, Map<QName, Serializable> props)
  {
      Map<QName, Serializable> result = new HashMap<>();
      
      for (Entry<QName, Serializable> entry : props.entrySet())
      {
          if (qnameList.contains(entry.getKey()))
          {
              result.put(entry.getKey(), entry.getValue());
          }
      }
      
      return result;
  }
  public void setAlfrescoServices(AlfrescoServices alfrescoServices) {
    searchService = alfrescoServices.getSearchService();
    namespaceService = alfrescoServices.getNamespaceService();
    nodeService = alfrescoServices.getNodeService();
    repositoryHelper = alfrescoServices.getRepositoryHelper();
    fileFolderService = alfrescoServices.getFileFolderService();
    behaviourFilter = alfrescoServices.getPolicyBehaviourFilter();
    dictionaryService = alfrescoServices.getDictionaryService();
    versionService = alfrescoServices.getVersionService();
    copyService = alfrescoServices.getCopyService();
    contentService = alfrescoServices.getContentService();
    mimetypeService = alfrescoServices.getMimetypeService();
    lockService = alfrescoServices.getLockService();
  }

  
  /**
   * For a node get SSID from a parent.  Iterate up through the nodes parent tree until 
   * you get to a node with an ssId set or the source sync node where it should be set.
   *  
   * @param targetSynchMemberNode
   */
  public String getSynSetDefID(NodeRef nodeRef){
    
    if(logger.isDebugEnabled()){logger.debug("getSyncSetDefId: entered");}
    
    String ssId = null;
    NodeRef parent = null;
    
    
    parent = this.nodeService.getPrimaryParent(nodeRef).getParentRef();
    
    while(ssId == null){
    
    
        if(this.nodeService.hasAspect(parent,SeedSynchModel.ASPECT_SYNCH_MEMBER_NODE)){
          ssId = (String)this.nodeService.getProperty(parent, SeedSynchModel.PROP_SYNCH_DEF_NODE_ID);
          if(logger.isDebugEnabled()){logger.debug("getSynSetDefID: " + ssId);}
          
          if(ssId != null)
            break;
        }
        
        if(nodeService.hasAspect(parent, SeedSynchModel.ASPECT_SYNCH_SOURCE)){
          
          if(logger.isDebugEnabled()){logger.debug("getSynSetDefID: Source Root Node Reached: " + ssId);}
          break;
        }
        
        
        // move up to next parent
        parent = this.nodeService.getPrimaryParent(parent).getParentRef();
    
    }
  
    return ssId;
    
  }

  /**
   * Create all nodes in target that are referenced in source.  Each node in the source should be updated with equivalent copied node in target.
   * 
   * This node is equilalent to fileFolerServer.copy but allows us to do linking back to source and also to account for things such as working copies which will be skipped
   * 
   * @param sourceNodeRef - Folder to start copying nodes from taget into 
   * @param targetNodeRef - Folder to start creating tree in target
   * @param ssdProps
   * @throws FileNotFoundException 
   * @throws FileExistsException 
   */
  public void copyTreeToTarget(NodeRef sourceFolderRef,    
      NodeRef targetFolderRef, boolean includeSubFolders, Map<QName, Serializable> ssdProps) throws FileExistsException, FileNotFoundException {
    
    this.disableSynchBehaviours();
    
    String ssdId = (String)ssdProps.get(SeedSynchModel.PROP_SYNCH_SET_ID);
    
    // iterate through source creating the equivalent node in target
    // update each source node with target nodeRef.
    
    // copy each document to the target
    List<FileInfo> srcFiles = fileFolderService.listFiles(sourceFolderRef);
    for (FileInfo file : srcFiles) {
        NodeRef fileNodeRef = file.getNodeRef();
        
        if(logger.isDebugEnabled()){logger.debug("createTreeInTarget: copy srce file to target: name" + file.getName() + " --> " + file.getNodeRef());}
        
        // only creat file if it does not already exist and it is not a working copy
        if(!targetExists(file.getNodeRef()) && !nodeService.hasAspect(file.getNodeRef(), ContentModel.ASPECT_WORKING_COPY)){
          // replace with copy file method.
          NodeRef fileRef = createTargetFileFromSource(fileNodeRef, targetFolderRef, ssdId);
              //fileFolderService.copy(fileNodeRef, targetFolderRef, null);
          
          if(logger.isDebugEnabled()){logger.debug("createTreeInTarget: Copied srce file to target: name" + nodeService.getProperty(fileRef, ContentModel.PROP_NAME) + " --> " + fileRef);}
          
        }
        
    }
    
    if(includeSubFolders){
      // process each folder, create a folder copy in target and the recurse method to process that folder.
      NodeRef newTargetFolderRef = null;
      List<FileInfo> srcFolders = fileFolderService.listFolders(sourceFolderRef);
      for (FileInfo folder : srcFolders) {
          NodeRef srcFolderNodeRef = folder.getNodeRef();
          
          if(logger.isDebugEnabled()){logger.debug("createTreeInTarget: copy srce folder to target " + folder.getName() + "-->" +folder.getNodeRef());}
          
          // rpelace with copy file method.
          if(!targetExists(folder.getNodeRef())){
              newTargetFolderRef = createTargetFolderFromSource(srcFolderNodeRef, targetFolderRef, ssdId);
              
          }
          
          // call method recursively
          copyTreeToTarget(srcFolderNodeRef, newTargetFolderRef,includeSubFolders, ssdProps);
      }
    }
    
    this.enableSynchBehaviours();;
    
  }
  
  /**
   * Recursive Add a blank Member Aspect to the tree of nodes
   * @param targetSynchMemberNode
   */
  public void addMemberPropertiesRecursively(NodeRef nodeRef){
    
    if(!nodeService.hasAspect(nodeRef, SeedSynchModel.ASPECT_SYNCH_MEMBER_NODE)){
      
        this.addMemberDetailsProperties(nodeRef, null, null);
    }
    
    List<FileInfo> children = fileFolderService.list(nodeRef);
    
    if(!children.isEmpty()){
      
      for (FileInfo child : children){
           
           NodeRef childNodeRef = child.getNodeRef();
           
           this.addMemberDetailsProperties(childNodeRef, null, null);
           
           if(child.isFolder()){
               addMemberPropertiesRecursively(childNodeRef);
             }
      
      }
     }else{
        if(logger.isDebugEnabled()) {logger.debug("No children for node.."  + nodeRef);}
      }
     
    
  }

  /**
   * Check if the source node exists in the target Tree
   * @param nodeRef
   * @return
   */
  private boolean targetExists(NodeRef nodeRef) {
    // TODO Auto-generated method stub
   boolean targetExists = false;
    
   String targetNodeRefStr = (String)this.nodeService.getProperty(nodeRef, SeedSynchModel.PROP_SYNCED_TARGET_REF);
   if(targetNodeRefStr != null){
    
      NodeRef targetNodeRef = new NodeRef(targetNodeRefStr);
    
    
      if(nodeService.exists(targetNodeRef)){
        if(logger.isDebugEnabled()){logger.debug("targetExists: true for nodeRef" + targetNodeRefStr);}
        targetExists = true;
      }
    }
    return targetExists;
  }

    /** 
     * Return whether a Node is currently locked
     * @param node             The Node wrapper to test against
     * @param lockService      The LockService to use
     * @return whether a Node is currently locked
     */
    public Boolean isNodeLocked(NodeRef nodeRef){
      Boolean locked=Boolean.FALSE;
      
      if (this.nodeService.hasAspect(nodeRef, ContentModel.ASPECT_LOCKABLE)) {
        LockStatus lockStatus = lockService.getLockStatus(nodeRef);
        if (lockStatus == LockStatus.LOCKED || lockStatus == LockStatus.LOCK_OWNER) {
          locked=Boolean.TRUE;
        }
      }
      return locked;
    }
    
    /** 
     * Return whether a Node is currently locked
     * @param node             The Node wrapper to test against
     * @param lockService      The LockService to use
     * @return whether a Node is currently locked
     */
    public void unlockNode(NodeRef nodeRef, boolean syncLockOwner){
      
      if(this.isNodeLocked(nodeRef) && syncLockOwner){
        lockService.unlock(nodeRef);
      }
    }

}
