package org.jboss.mbui.gui.behaviour;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HasHandlers;
import org.jboss.mbui.model.structure.QName;

/**
 * A navigation event is the created by a {@link ModelDrivenCommand}.
 * It's an instruction to navigate to a different interaction unit.
 *
 * @author Heiko Braun
 * @date 11/15/12
 */
public class NavigationEvent extends GwtEvent<NavigationEvent.Handler> {

    public static final Type TYPE = new Type<Handler>();

    private QName id;

    private QName target;

    public NavigationEvent(QName id, QName target) {
        super();
        this.id = id;
        this.target = target;
    }

    public QName getId() {
        return id;
    }

    public QName getTarget() {
        return target;
    }

    @Override
    public Type<Handler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(Handler listener) {
        if(listener.accepts(this))
            listener.onNavigationEvent(this);
    }

    public interface Handler extends EventHandler {
        boolean accepts(NavigationEvent event);
        void onNavigationEvent(NavigationEvent event);
    }

    public static void fire(HasHandlers source, NavigationEvent eventInstance) {
        source.fireEvent(eventInstance);
    }

    @Override
    public String toString() {
        return "NavigationEvent{" +
                "id=" + id +
                ", target=" + target +
                '}';
    }
}
