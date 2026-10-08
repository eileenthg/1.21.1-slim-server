package net.eileenthg.colonyexpeditions.core;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.core.colony.jobs.JobNetherWorker;
import com.minecolonies.core.entity.ai.workers.production.EntityAIWorkNether;

/** Uses the Netherworker model and job data; only the AI differs. */
public class JobExpedition extends JobNetherWorker
{
    public JobExpedition(final ICitizenData entity)
    {
        super(entity);
    }

    @Override
    public EntityAIWorkNether generateAI()
    {
        return new EntityAIExpedition(this);
    }
}
