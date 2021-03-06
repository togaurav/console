/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.console.client.tools.mbui.workbench.editor;

import com.allen_sauer.gwt.log.client.Log;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyStandard;
import com.gwtplatform.mvp.client.proxy.ProxyPlace;
import com.gwtplatform.mvp.client.proxy.RevealContentEvent;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.shared.dispatch.DispatchAsync;
import org.jboss.as.console.client.shared.subsys.tx.model.TransactionManager;
import org.jboss.as.console.client.tools.mbui.workbench.ApplicationPresenter;
import org.jboss.as.console.client.tools.mbui.workbench.ReifyEvent;
import org.jboss.as.console.client.tools.mbui.workbench.ResetEvent;
import org.jboss.as.console.client.tools.mbui.workbench.repository.DataSourceSample;
import org.jboss.as.console.client.tools.mbui.workbench.repository.Sample;
import org.jboss.as.console.client.tools.mbui.workbench.repository.TransactionSample;
import org.jboss.as.console.client.widgets.forms.ApplicationMetaData;
import org.jboss.as.console.client.widgets.forms.EntityAdapter;
import org.jboss.mbui.gui.behaviour.InteractionCoordinator;
import org.jboss.mbui.gui.behaviour.Precondition;
import org.jboss.mbui.gui.behaviour.Procedure;
import org.jboss.mbui.gui.behaviour.StatementContext;
import org.jboss.mbui.gui.behaviour.as7.LoadResourceProcedure;
import org.jboss.mbui.gui.behaviour.as7.SaveChangesetProcedure;
import org.jboss.mbui.gui.reification.Context;
import org.jboss.mbui.gui.reification.ReificationPipeline;
import org.jboss.mbui.gui.reification.strategy.ContextKey;
import org.jboss.mbui.gui.reification.strategy.ReificationWidget;
import org.jboss.mbui.model.structure.InteractionUnit;
import org.jboss.mbui.model.structure.QName;

import java.util.HashMap;
import java.util.Map;

import static org.jboss.as.console.client.tools.mbui.workbench.NameTokens.preview;

/**
 *
 * @author Harald Pehl
 * @author Heiko Braun
 *
 * @date 10/30/2012
 */
public class PreviewPresenter extends Presenter<PreviewPresenter.MyView, PreviewPresenter.MyProxy>
        implements ReifyEvent.ReifyHandler, ResetEvent.Handler
{

    private Map<String, InteractionCoordinator> coordinators = new HashMap<String, InteractionCoordinator>();
    private String selectedSample = null;
    private final ReificationPipeline reificationPipeline;
    private DispatchAsync dispatcher;
    private HashMap<String, ReificationWidget> cachedWidgets = new HashMap<String, ReificationWidget>();
    private final ApplicationMetaData metaData;
    private final EntityAdapter<TransactionManager> txAdapter;

    public interface MyView extends View
    {
        void show(ReificationWidget interactionUnit);
    }

    @ProxyStandard
    @NameToken(preview)
    public interface MyProxy extends ProxyPlace<PreviewPresenter>
    {
    }

    @Inject
    public PreviewPresenter(
            final EventBus eventBus, final MyView view,
            final MyProxy proxy, final ReificationPipeline reificationPipeline,
            final ApplicationMetaData metaData,
            final DispatchAsync dispatcher)
    {
        super(eventBus, view, proxy);
        this.reificationPipeline = reificationPipeline;
        this.metaData = metaData;
        this.dispatcher = dispatcher;

        this.txAdapter = new EntityAdapter<TransactionManager>(TransactionManager.class, metaData);

        // these would be created/stored differently. This is just an example
        final TransactionSample transactionSample = new TransactionSample();
        final DataSourceSample dataSourceSample = new DataSourceSample();

        StatementContext statementContext = new StatementContext() {
            @Override
            public String resolve(String key) {
                String resolvedValue = null;
                if("selected.profile".equals(key))
                    resolvedValue = Console.MODULES.getCurrentSelectedProfile().getName();
                return resolvedValue;
            }
        };

        final InteractionCoordinator txCoordinator = new InteractionCoordinator(transactionSample.getDialog(), statementContext);
        final InteractionCoordinator dsCoordinator = new InteractionCoordinator(dataSourceSample.getDialog(), statementContext);

        coordinators.put(transactionSample.getName(), txCoordinator);
        coordinators.put(dataSourceSample.getName(), dsCoordinator);

        // setup behaviour hooks
        final QName datasourcesResource = new QName("org.jboss.datasource", "datasources");
        final QName transactionManagerResource = new QName("org.jboss.transactions", "transactionManager");

        // --------- TX behaviour ------------
        Procedure saveTxAttributes = new SaveChangesetProcedure(
                transactionManagerResource,
                dispatcher);

        Procedure loadTxAttributes = new LoadResourceProcedure(
                transactionManagerResource,
                dispatcher);

        txCoordinator.registerProcedure(saveTxAttributes);
        txCoordinator.registerProcedure(loadTxAttributes);

        // --------- DS behaviour ------------

        final Precondition selectedEntity = new Precondition() {
            @Override
            public boolean isMet(StatementContext statementContext) {
                return statementContext.resolve("selected.entity")!=null;
            }
        };

        Procedure saveDsAttributes = new SaveChangesetProcedure(
                datasourcesResource,
                dispatcher);

        Procedure loadDatasources = new LoadResourceProcedure(
                datasourcesResource,
                dispatcher);

        Procedure loadDatasource = new LoadResourceProcedure(
                       QName.valueOf("org.jboss.datasource:datasource"),
                       dispatcher);
        loadDatasource.setPrecondition(selectedEntity);

        dsCoordinator.registerProcedure(saveDsAttributes);
        dsCoordinator.registerProcedure(loadDatasources);
        dsCoordinator.registerProcedure(loadDatasource);

    }

    public DispatchAsync getDispatcher() {
        return dispatcher;
    }

    private InteractionCoordinator getActiveCoordinator()
    {
        if(null==selectedSample)
            throw new RuntimeException("No sample selected (requires reification/onBind)");

        return coordinators.get(selectedSample);
    }

    @Override
    protected void onBind() {
        super.onBind();
        getEventBus().addHandler(ReifyEvent.getType(), this);
        getEventBus().addHandler(ResetEvent.TYPE, this);
    }

    @Override
    protected void revealInParent()
    {
        RevealContentEvent.fire(this, ApplicationPresenter.TYPE_SetMainContent, this);
    }

    // in real this would be wired to Presenter.onBind()
    @Override
    public void onReify(final ReifyEvent event)
    {
        // TODO: dialog models would ned to be stored for later retrieval in a real world app
        Sample sample = event.getSample();
        selectedSample = sample.getName();

        if(cachedWidgets.get(selectedSample)==null)
        {
            InteractionUnit interactionUnit = sample.getDialog().getInterfaceModel();
            final Context context = new Context();

            // make the coordinator bus available to the model components
            context.set(ContextKey.COORDINATOR, getActiveCoordinator().getLocalBus());

            reificationPipeline.execute(interactionUnit, context, new SimpleCallback<Boolean>()
            {
                @Override
                public void onSuccess(final Boolean successful)
                {
                    if (successful)
                    {
                        ReificationWidget widget = context.get(ContextKey.WIDGET);
                        if (widget != null)
                        {
                            cachedWidgets.put(selectedSample, widget);
                            getView().show(widget);
                        }
                    }
                    else
                    {
                        Log.error("Reification failed");
                    }
                }
            });
        }
        else
        {
            getView().show(cachedWidgets.get(selectedSample));
        }
    }

    // in a real this would be wired Presenter.onReset()
    @Override
    public void doReset() {
        getActiveCoordinator().onReset();
    }
}
