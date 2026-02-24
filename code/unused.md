/*Warehouse warehouse = new Warehouse();

    warehouse.addDeliveryToStagingArea(delivery.getDeliveryItems());
    //warehouse.printStagingArea();

    Stocker stocker = new Stocker();
    stocker.pickupItemsFromStagingArea(warehouse.getStagingArea());
    util.printEventWithBoxes("130", "S1", "stocker_load", stocker.getStockerInventory(), true);

    stocker.stockItemsToWarehouse(warehouse.getInventory());
    Picker picker = new Picker();
    picker.takeBox(warehouse.getInventory());

    util.printPickEvent("140", "P7", "pick_start", "88", picker.getPickerInventory());

    util.printPickEvent("150", "P7", "pick_done", "88", picker.getPickerInventory());

    //warehouse.printStagingArea();

    /*warehouse.addItem("electronics", 7);
    warehouse.addItem("books", 0);

    warehouse.printInventory();

    warehouse.printEvent("120", "DEL", "delivery_arrived", warehouse.getInventory());*/