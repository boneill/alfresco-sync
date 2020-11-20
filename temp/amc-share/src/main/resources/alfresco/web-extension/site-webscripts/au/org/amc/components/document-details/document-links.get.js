<import resource="classpath:/alfresco/templates/org/alfresco/import/alfresco-util.js">

function main()
{
   AlfrescoUtil.param('nodeRef');
   AlfrescoUtil.param('site', null);
   var documentDetails = AlfrescoUtil.getNodeDetails(model.nodeRef, model.site);
   if (documentDetails)
   {
      model.document = documentDetails.item;
      model.repositoryUrl = AlfrescoUtil.getRepositoryUrl();
   }
   //Get actual node and path to widget
   var docNode = AlfrescoUtil.getNodeDetails(model.nodeRef)
   model.docNode = docNode;
   // Widget instantiation metadata...
   var documentActions = {
      id: "customDocumentLinks", 
      name: "Alfresco.customDocumentLinks",
      options: {
         nodeRef: model.nodeRef,
         siteId: model.site,
         documentNode: model.docNode
      }
   };
   model.widgets = [documentActions];
}

main();