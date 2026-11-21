package com.bandybus.infwhat.botania.network;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Runs packet work on the server thread.
 *
 * <p>1.7.10 has no {@code IThreadListener}, so the SimpleNetworkWrapper handlers execute on the
 * Netty thread. Every handler that touches a world or a tile entity posts its work here instead;
 * the queue is drained at the end of every server tick.
 */
public final class ServerTaskQueue {

    public static final ServerTaskQueue INSTANCE = new ServerTaskQueue();

    private final Queue<Runnable> tasks = new ConcurrentLinkedQueue<Runnable>();

    private ServerTaskQueue() {
    }

    public static void register() {
        FMLCommonHandler.instance().bus().register(INSTANCE);
    }

    public static void enqueue(Runnable task) {
        INSTANCE.tasks.add(task);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Runnable task;
        while ((task = tasks.poll()) != null) {
            task.run();
        }
    }
}
