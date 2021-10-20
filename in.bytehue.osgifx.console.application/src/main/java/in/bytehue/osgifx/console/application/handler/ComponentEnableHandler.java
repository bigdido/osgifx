package in.bytehue.osgifx.console.application.handler;

import static in.bytehue.osgifx.console.event.topics.ComponentActionEventTopics.COMPONENT_ENABLED_EVENT_TOPIC;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import in.bytehue.osgifx.console.agent.Agent;
import in.bytehue.osgifx.console.supervisor.Supervisor;

public final class ComponentEnableHandler {

    @Log
    @Inject
    private FluentLogger logger;
    @Inject
    private IEventBroker eventBroker;
    @Inject
    private Supervisor   supervisor;

    @Execute
    public void execute(@Named("name") final String name) {
        final Agent agent = supervisor.getAgent();
        if (supervisor.getAgent() == null) {
            logger.atWarning().log("Remote agent cannot be connected");
            return;
        }
        try {
            final String error = agent.enableComponent(name);
            if (error == null) {
                logger.atInfo().log("Component with name '%s' has been enabled", name);
                eventBroker.send(COMPONENT_ENABLED_EVENT_TOPIC, name);
            } else {
                logger.atError().log(error);
            }
        } catch (final Exception e) {
            logger.atError().withException(e).log("Component with name '%s' cannot be enabled", name);
        }
    }

}