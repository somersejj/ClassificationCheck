package org.bimserver.classificationcheck;
 
import org.bimserver.interfaces.objects.SInternalServicePluginConfiguration;
import org.bimserver.interfaces.objects.SObjectType;
import org.bimserver.models.log.AccessMethod;
import org.bimserver.models.store.ObjectDefinition;
import org.bimserver.models.store.ServiceDescriptor;
import org.bimserver.models.store.StoreFactory;
import org.bimserver.models.store.Trigger;
import org.bimserver.plugins.PluginConfiguration;
import org.bimserver.plugins.PluginContext;
import org.bimserver.plugins.services.BimServerClientInterface;
import org.bimserver.plugins.services.NewRevisionHandler;
import org.bimserver.plugins.services.ServicePlugin;
import org.bimserver.shared.exceptions.PluginException;
import org.bimserver.shared.exceptions.ServerException;
import org.bimserver.shared.exceptions.UserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClassificationCheckServicePlugin extends ServicePlugin {

	private static final Logger LOGGER = LoggerFactory.getLogger(ClassificationCheckServicePlugin.class);

	@Override
	public void init(PluginContext pluginContext) throws PluginException {
		super.init(pluginContext);
	}

	public void register(long uoid, SInternalServicePluginConfiguration internalServicePluginConfiguration, final PluginConfiguration pluginConfiguration) {
		ServiceDescriptor classificationCheck = StoreFactory.eINSTANCE.createServiceDescriptor();
		classificationCheck.setProviderName("BIMserver");
		classificationCheck.setIdentifier("" + internalServicePluginConfiguration.getOid());
		classificationCheck.setName("ClassificationCheck");
		classificationCheck.setDescription("ClassificationCheck");
		classificationCheck.setNotificationProtocol(AccessMethod.INTERNAL);
		classificationCheck.setReadRevision(true);
		classificationCheck.setWriteExtendedData("http://bimserver.org/classificationCheck");
		classificationCheck.setTrigger(Trigger.NEW_REVISION);

		registerNewRevisionHandler(uoid, classificationCheck, new ClassificationNewRevisionHandler());
		
	}

	@Override
	public String getTitle() {
		return "ClassificationCheck";
	}

	@Override
	public ObjectDefinition getSettingsDefinition() {
		ObjectDefinition objectDefinition = StoreFactory.eINSTANCE.createObjectDefinition();
		return objectDefinition;
	}

	@Override
	public void unregister(SInternalServicePluginConfiguration internalService) {
	}

}

