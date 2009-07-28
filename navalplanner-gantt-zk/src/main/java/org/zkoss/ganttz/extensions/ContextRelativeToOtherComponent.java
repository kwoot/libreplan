package org.zkoss.ganttz.extensions;

import org.zkoss.ganttz.adapters.PlannerConfiguration;
import org.zkoss.zk.ui.Component;

/**
 * An implementation of {@link IContext} that delegates to another context and
 * redefines its {@link IContext#getRelativeTo()}
 * @author Óscar González Fernández <ogonzalez@igalia.com>
 */
public class ContextRelativeToOtherComponent<T> implements IContext<T> {

    private final Component component;
    private final IContext<T> context;

    public static <T> IContext<T> makeRelativeTo(IContext<T> context,
            Component component) {
        return new ContextRelativeToOtherComponent<T>(component, context);
    }

    private ContextRelativeToOtherComponent(Component component,
            IContext<T> context) {
        if (component == null)
            throw new IllegalArgumentException("component must be not null");
        if (context == null)
            throw new IllegalArgumentException("context must be not null");
        this.component = component;
        this.context = context;
    }

    public void add(T domainObject) {
        context.add(domainObject);
    }

    public Component getRelativeTo() {
        return component;
    }

    public void reload(PlannerConfiguration<?> configuration) {
        context.reload(configuration);
    }

    public void remove(T domainObject) {
        context.remove(domainObject);
    }

}
