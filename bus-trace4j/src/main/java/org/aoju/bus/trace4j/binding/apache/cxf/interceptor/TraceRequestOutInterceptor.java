package org.aoju.bus.trace4j.binding.apache.cxf.interceptor;

import org.aoju.bus.trace4j.TraceBackend;
import org.aoju.bus.trace4j.config.TraceFilterConfiguration;
import org.aoju.bus.trace4j.consts.TraceConsts;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.Phase;


public class TraceRequestOutInterceptor extends AbstractTraceOutInterceptor {

    public TraceRequestOutInterceptor(TraceBackend backend) {
        this(backend, TraceConsts.DEFAULT);
    }

    public TraceRequestOutInterceptor(TraceBackend backend, String profile) {
        super(Phase.USER_LOGICAL, TraceFilterConfiguration.Channel.OutgoingRequest, backend, profile);
    }

    @Override
    protected boolean shouldHandleMessage(Message message) {
        return MessageUtils.isRequestor(message);
    }

}