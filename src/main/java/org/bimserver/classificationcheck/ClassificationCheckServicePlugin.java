package org.bimserver.classificationcheck;
 
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bimserver.emf.IdEObject;
import org.bimserver.emf.IfcModelInterface;
import org.bimserver.interfaces.objects.SActionState;
import org.bimserver.interfaces.objects.SExtendedData;
import org.bimserver.interfaces.objects.SExtendedDataSchema;
import org.bimserver.interfaces.objects.SFile;
import org.bimserver.interfaces.objects.SInternalServicePluginConfiguration;
import org.bimserver.interfaces.objects.SLongActionState;
import org.bimserver.interfaces.objects.SObjectType;
import org.bimserver.interfaces.objects.SProgressTopicType;
import org.bimserver.interfaces.objects.SProject;
import org.bimserver.models.ifc2x3tc1.IfcClassification;
import org.bimserver.models.ifc2x3tc1.IfcRelAssociatesClassification;
import org.bimserver.models.ifc2x3tc1.IfcRoot;
import org.bimserver.models.log.AccessMethod;
import org.bimserver.models.store.ObjectDefinition;
import org.bimserver.models.store.ServiceDescriptor;
import org.bimserver.models.store.StoreFactory;
import org.bimserver.models.store.Trigger;
import org.bimserver.plugins.PluginConfiguration;
import org.bimserver.plugins.PluginException;
import org.bimserver.plugins.PluginManager;
import org.bimserver.plugins.services.BimServerClientException;
import org.bimserver.plugins.services.BimServerClientInterface;
import org.bimserver.plugins.services.NewRevisionHandler;
import org.bimserver.plugins.services.ServicePlugin;
import org.bimserver.shared.PublicInterfaceNotFoundException;
import org.bimserver.shared.exceptions.ServerException;
import org.bimserver.shared.exceptions.UserException;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.collect.BiMap;

public class ClassificationCheckServicePlugin extends ServicePlugin {

	private static final Logger LOGGER = LoggerFactory.getLogger(ClassificationCheckServicePlugin.class);
	private boolean initialized;
	private static final String NAMESPACE = "http://bimserver.org/classificationCheck";

	@Override
	public void init(final PluginManager pluginManager) throws PluginException {
		super.init(pluginManager);
		initialized = true;
	}

	public void register(long uoid, SInternalServicePluginConfiguration internalServicePluginConfiguration, final PluginConfiguration pluginConfiguration) {
		ServiceDescriptor classificationCheck = StoreFactory.eINSTANCE.createServiceDescriptor();
		classificationCheck.setProviderName("BIMserver");
		classificationCheck.setIdentifier("" + internalServicePluginConfiguration.getOid());
		classificationCheck.setName("ClassificationCheck");
		classificationCheck.setDescription("ClassificationCheck");
		classificationCheck.setNotificationProtocol(AccessMethod.INTERNAL);
		classificationCheck.setReadRevision(true);
		classificationCheck.setWriteExtendedData(NAMESPACE);
		classificationCheck.setTrigger(Trigger.NEW_REVISION);
		
		
		registerNewRevisionHandler(uoid, classificationCheck, new NewRevisionHandler() {
			public void newRevision(BimServerClientInterface bimServerClientInterface, long poid, long roid, String userToken, long soid, SObjectType settings) throws ServerException, UserException {
				
				LOGGER.info("Classification check is called");
			
				try {

					Date startDate = new Date(); 
					Long topicId = bimServerClientInterface.getRegistry().registerProgressOnRevisionTopic(SProgressTopicType.RUNNING_SERVICE, poid, roid, "Running Classification Checker");
					SLongActionState state = new SLongActionState();
					state.setTitle("Classification checker");
					state.setState(SActionState.STARTED);
					state.setProgress(-1);
					state.setStart(startDate);
					bimServerClientInterface.getRegistry().updateProgressTopic(topicId, state);
					
					StringBuffer info = new StringBuffer();
					info.append("Classification check results : \n");
					SProject project;
					project = bimServerClientInterface.getBimsie1ServiceInterface().getProjectByPoid(poid);
					IfcModelInterface model =  bimServerClientInterface.getModel(project, roid, true, true);
					ArrayList<Long> classifiedList = new ArrayList<Long>();
                    
					int counter = 0;
					for (IfcRelAssociatesClassification ifcRelAssociatesClassification : model.getAllWithSubTypes(IfcRelAssociatesClassification.class))
					{
						EList<IfcRoot> a = ifcRelAssociatesClassification.getRelatedObjects();
						for (IfcRoot ifcRoot : a)
						{
							LOGGER.debug("found object in IfcRelAssociatesClassification list: " + ifcRoot.getOid() + " : " + model.get(ifcRoot.getOid()).toString() );	
							if (!classifiedList.contains(ifcRoot.getOid()))
							{
								classifiedList.add(ifcRoot.getOid());
								counter++;
						    }
						}  
					}
					LOGGER.debug("Objects found with classification: " + counter);	
					info.append("Objects found with classification: " + counter + "\n");

					BiMap<Long,IdEObject> objects = model.getObjects();

					LOGGER.debug("Total objects in the model: " + objects.size());	
					LOGGER.debug("Total objects without classification: " + (objects.size() - counter));	
					info.append("Total objects in the model: " + objects.size() + "\n");
					info.append("Total objects without classification: " + (objects.size() - counter) + "\n");
					
					for (Long key : objects.keySet())
					{
						IdEObject object = objects.get(key);
						//LOGGER.info("found object " + object.getOid() + "  --  " + key);
						
                        if (!classifiedList.contains(object.getOid()))
                        {
                        	// this object has no classification
        					LOGGER.debug("Object: "+ object.getClass().getName() + "(" + (object.getOid())+") has no classification.");	
        					info.append("Object: "+ object.getClass().getName() + "(" + (object.getOid())+") has no classification. \n");
                        }
					}
					
					List<java.lang.String> infos = new ArrayList<String>();
					infos.add(info.toString());
					
					state = new SLongActionState();
					state.setProgress(100);
					state.setInfos(infos);
					state.setTitle("BuildingStorey checker");
					state.setState(SActionState.FINISHED);
					state.setStart(startDate);
					state.setEnd(new Date());
					bimServerClientInterface.getRegistry().updateProgressTopic(topicId, state);
					bimServerClientInterface.getRegistry().unregisterProgressTopic(topicId);
				   
					LOGGER.info("Writing classification check is results to extended data");
					
					addExtendedData(info.toString().getBytes(Charsets.UTF_8), "BuildingStoreycheckerlog.txt", "BuildingStoreycheckerlog", "text", bimServerClientInterface, roid);

					LOGGER.info("Classification check is done.");

				} catch (BimServerClientException
						| PublicInterfaceNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
	}

	public void addExtendedData(byte[] data, String filename, String title, String mime, BimServerClientInterface bimServerClientInterface, long roid) {
		try {
			
			SExtendedDataSchema extendedDataSchemaByNamespace = bimServerClientInterface.getBimsie1ServiceInterface().getExtendedDataSchemaByNamespace(
					NAMESPACE);

			SFile file = new SFile();
			SimpleDateFormat sdf = new SimpleDateFormat("YYYY-dd-MM HH:mm");
			SExtendedData extendedData = new SExtendedData();
			extendedData.setTitle("classificationCheck Results(" + sdf.format(new Date()) + ")");
			file.setFilename("classificationCheck.txt");
			extendedData.setSchemaId(extendedDataSchemaByNamespace.getOid());
			try {
				file.setData(data);
				file.setMime(mime);

				long fileId = bimServerClientInterface.getServiceInterface().uploadFile(file);
				extendedData.setFileId(fileId);
				bimServerClientInterface.getBimsie1ServiceInterface().addExtendedDataToRevision(roid, extendedData);
			} catch (Exception e) {
				LOGGER.error("", e);
			}
		} catch (Exception e) {
			LOGGER.error("", e);
		}
	}

	@Override
	public String getDescription() {
		return "Provides classification check service";
	}

	@Override
	public String getVersion() {
		return "1.0";
	}

	@Override
	public boolean isInitialized() {
		return initialized;
	}

	@Override
	public String getTitle() {
		return "ClassificationCheck";
	}

	@Override
	public String getDefaultName() {
		return "ClassificationCheck";
	}

	@Override
	public void unregister(SInternalServicePluginConfiguration internalService) {
	}

	@Override
	public ObjectDefinition getSettingsDefinition() {
		// TODO Auto-generated method stub
		return null;
	}
	
	
}