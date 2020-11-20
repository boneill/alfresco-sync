package au.com.seedim.synch.model;

import org.alfresco.service.namespace.QName;

public interface SeedSynchModel {

  //URL
  static final String SEED_SYNCH_MODEL_1_0_URI = "http://www.seedim.com.au/model/synch/1.0";
  
  
  //Types
  static final QName TYPE_SYNCH_SET_DEFINITION = QName.createQName(SEED_SYNCH_MODEL_1_0_URI, "syncSetDefinitionType");
  
  //Aspects
  static final QName ASPECT_SYNCH_SOURCE = QName.createQName(SEED_SYNCH_MODEL_1_0_URI, "synchSourceAspect");
  static final QName ASPECT_SYNCH_TARGET = QName.createQName(SEED_SYNCH_MODEL_1_0_URI, "synchTargetAspect");
  static final QName ASPECT_SYNCH_MEMBER_NODE = QName.createQName(SEED_SYNCH_MODEL_1_0_URI, "synchMemberNodeAspect");
  static final QName ASPECT_SYNCH_FAILED = QName.createQName(SEED_SYNCH_MODEL_1_0_URI, "synchFailedAspect");
  
  //Properties
  
  
  static final QName PROP_SYNCH_SET_ID= QName.createQName(SEED_SYNCH_MODEL_1_0_URI, "synchSetID");
  static final QName PROP_SYNCH_SET_CREATOR= QName.createQName(SEED_SYNCH_MODEL_1_0_URI, "synchSetCreator");
  static final QName PROP_SYNCH_SET_TARGET_ROOT_FOLDER= QName.createQName(SEED_SYNCH_MODEL_1_0_URI, "synchSetTargetRootFolder");
  static final QName PROP_SYNCH_SET_INCLUDE_SUBFOLDERS= QName.createQName(SEED_SYNCH_MODEL_1_0_URI, "synchSetIncludeSubFolders");
  static final QName PROP_SYNCH_SET_JOB_STATUS= QName.createQName(SEED_SYNCH_MODEL_1_0_URI, "synchSetJobStatus");
  
  
  
  static final QName PROP_CREATOR_USERNAME = QName.createQName(SEED_SYNCH_MODEL_1_0_URI, "creatorUserName");
  static final QName PROP_INCLUDE_SUBFOLDERS = QName.createQName(SEED_SYNCH_MODEL_1_0_URI, "includeSubfolders");
  
  static final QName PROP_TARGET_SYNCH_DATE = QName.createQName(SEED_SYNCH_MODEL_1_0_URI, "synchDate");
  static final QName PROP_TARGET_SYNCH_DEF_NODE_ID = QName.createQName(SEED_SYNCH_MODEL_1_0_URI, "targetSynchSetDefID");
  
  static final QName PROP_IS_SYNCHED= QName.createQName(SEED_SYNCH_MODEL_1_0_URI, "isSynched");
  static final QName PROP_SYNCH_DEF_NODE_ID = QName.createQName(SEED_SYNCH_MODEL_1_0_URI, "synchSetDefID");
  static final QName PROP_HAS_PROPERTIES_CHANGED = QName.createQName(SEED_SYNCH_MODEL_1_0_URI, "hasPropertiesChanged");
  static final QName PROP_HAS_CONTENT_CHANGED = QName.createQName(SEED_SYNCH_MODEL_1_0_URI, "hasContentChanged");
  static final QName PROP_PREVIOUS_NAME = QName.createQName(SEED_SYNCH_MODEL_1_0_URI, "previousName");
  static final QName PROP_SYNCED_TARGET_REF = QName.createQName(SEED_SYNCH_MODEL_1_0_URI, "syncedTargetRef");
  
  
  
  
  
  static final QName PROP_SYNCH_ERROR_DETAILS= QName.createQName(SEED_SYNCH_MODEL_1_0_URI, "synchErrorDetails");
  static final QName PROP_SYNCH_ERROR_TIME = QName.createQName(SEED_SYNCH_MODEL_1_0_URI, "synchErrorTime");
  
  
  
  //Assocs
  static final QName ASSOC_TARGET_ROOT_FOLDER = QName.createQName(SEED_SYNCH_MODEL_1_0_URI, "targetRootFolderAssoc");
  static final QName ASSOC_SYNCH_SET_DEF_NODE = QName.createQName(SEED_SYNCH_MODEL_1_0_URI, "synchSetDefNodeAssoc");
  static final QName ASSOC_SOURCE_ROOT_FOLDER = QName.createQName(SEED_SYNCH_MODEL_1_0_URI, "sourceRootFolderAssoc");
 
  
}
