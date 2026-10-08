package net.eileenthg.maidchef;

import com.github.tartaricacid.touhoulittlemaid.api.ILittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.LittleMaidExtension;
import com.github.tartaricacid.touhoulittlemaid.api.entity.data.TaskDataKey;
import com.github.tartaricacid.touhoulittlemaid.entity.data.TaskDataRegister;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskManager;

@LittleMaidExtension
public final class ChefPlugin implements ILittleMaid {
    public static TaskDataKey<ChefData> CHEF_DATA;

    @Override
    public void addMaidTask(TaskManager manager) {
        manager.add(TaskChef.INSTANCE);
    }

    @Override
    public void registerTaskData(TaskDataRegister register) {
        CHEF_DATA = register.register(TaskChef.UID, ChefData.CODEC);
    }

    /** Talking to her about her cooking (TLM AI chat, e.g. Player2). */
    @Override
    public void registerAIMaidContext(com.github.tartaricacid.touhoulittlemaid.ai.agent.context.GameContextRegister register) {
        ChefChat.registerContexts(register);
    }

    @Override
    public void registerAITool(com.github.tartaricacid.touhoulittlemaid.ai.agent.tool.ToolRegister register) {
        ChefChat.registerTools(register);
    }
}
