package com.pickme.common.event;

import java.util.List;

public interface DomainEventProvider {

    List<DomainEvent> getDomainEvents();

    void clearDomainEvents();
}
