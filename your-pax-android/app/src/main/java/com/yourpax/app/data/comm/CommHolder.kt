package com.yourpax.app.data.comm

object CommHolder {
    var comm: CommunicationManager = CachedCommManager(BtCommManager("00:00:00:00:00:00"))
}
