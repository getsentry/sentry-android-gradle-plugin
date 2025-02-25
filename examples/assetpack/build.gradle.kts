plugins {
    id("com.android.asset-pack")
}

assetPack {
    packName.set("assetpack")
    dynamicDelivery {
        deliveryType.set("install-time")
    }
}
