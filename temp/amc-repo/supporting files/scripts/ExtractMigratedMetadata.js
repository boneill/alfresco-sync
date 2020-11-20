//var document = search.findNode("workspace://SpacesStore/931dc4d5-51b4-4e87-a27f-4710ea88c901");
//logger.log(document.name + " (" + document.mimetype + "): " + "dateMigrated:" + document.properties["amc:dateMigrated"] + "ceated:" + document.properties["amc:contentCreated"]);

var props = new Array(); 
var date = new Date();
var ISODate = utils.toISO8601(date);
props["amc:dateMigrated"] = ISODate;

amcJsUtils.disableAllBehaviours();
if(!document.hasAspect("amc:migrated")){
  document.addAspect("amc:migrated", props);
}
// extract metadata
if((document.mimetype == "application/msword")||(document.mimetype == "application/vnd.ms-excel")||(document.mimetype == "application/vnd.ms-powerpoint")||(document.mimetype == "application/vnd.openxmlformats-officedocument.wordprocessingml.document")||(document.mimetype == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")||(document.mimetype == "application/vnd.openxmlformats-officedocument.presentationml.presentation")){
  //logger.log("Is a doc");
  var action = actions.create("extract-metadata");
  action.execute(document);
  if(document.properties["amc:contentCreated"] != null){
  document.properties["cm:created"] = document.properties["amc:contentCreated"];
  }
  document.properties["cm:title"] = "";
  document.save();
  //Clean Up Node
  if(document.hasAspect("amc:migrated")){
  //document.removeProperty("amc:contentCreated");
  //document.removeProperty("amc:dateMigrated");
  delete document.properties["amc:contentCreated"];
  delete document.properties["amc:dateMigrated"];
  document.removeAspect("amc:migrated");
  document.save();
  }
}
amcJsUtils.enableAllBehaviours();