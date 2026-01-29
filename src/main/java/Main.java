import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Main {

    private final DataRetriever dataRetriever;

    public Main(DataRetriever dataRetriever) {
        this.dataRetriever = dataRetriever;
    }

    public void runTests() {
        System.out.println("=== PRÉPARATION : Ajout de stock pour les tests ===\n");
        addStockForTesting();

        System.out.println("\n=== TEST 1: Create a new order (status CREATED) ===\n");
        Order newOrder = createSimpleOrder();
        System.out.println("New order created:");
        printOrderDetails(newOrder);

        System.out.println("\n=== TEST 2: Update status to READY ===\n");
        newOrder.setStatus(OrderStatus.READY);
        Order orderReady = dataRetriever.saveOrder(newOrder);
        System.out.println("After changing to READY:");
        printOrderDetails(orderReady);

        System.out.println("\n=== TEST 3: Retrieve by reference ===\n");
        Order retrieved = dataRetriever.findOrderByReference(orderReady.getReference());
        System.out.println("Order retrieved from database:");
        printOrderDetails(retrieved);

        System.out.println("\n=== TEST 4: Change to DELIVERED ===\n");
        retrieved.setStatus(OrderStatus.DELIVERED);
        Order deliveredOrder = dataRetriever.saveOrder(retrieved);
        System.out.println("After marking as DELIVERED:");
        printOrderDetails(deliveredOrder);

        System.out.println("\n=== TEST 5: Try to modify after DELIVERED (should fail) ===\n");
        try {
            deliveredOrder.setStatus(OrderStatus.READY);
            deliveredOrder.setType(OrderType.TAKE_AWAY);
            dataRetriever.saveOrder(deliveredOrder);
            System.out.println("ERROR: modification succeeded but should have failed!");
        } catch (IllegalStateException e) {
            System.out.println("SUCCESS: exception correctly thrown → " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Unexpected error: " + e.getMessage());
        }

        System.out.println("\n=== TEST 6: Create a TAKE_AWAY order ===\n");
        Order takeawayOrder = createTakeAwayOrder();
        System.out.println("Take-away order created:");
        printOrderDetails(takeawayOrder);
    }

    private void addStockForTesting() {
        addStockToIngredient(1, 5.0, Unit.KG);   // Laitue   (pour salade)
        addStockToIngredient(2, 5.0, Unit.KG);   // Tomate   (pour salade)
        addStockToIngredient(3, 5.0, Unit.KG);   // Poulet   (pour plat principal)
        addStockToIngredient(4, 5.0, Unit.KG);   // Chocolat (pour gâteau)
        addStockToIngredient(5, 5.0, Unit.KG);   // Beurre   (pour gâteau)

        System.out.println("→ Stock ajouté : 5 kg pour chaque ingrédient clé");
        System.out.println("→ Les commandes devraient maintenant passer la vérification stock.");
    }

    private void addStockToIngredient(int ingredientId, double quantity, Unit unit) {
        Ingredient ing = dataRetriever.findIngredientById(ingredientId);
        if (ing == null) {
            System.out.println("Ingredient id " + ingredientId + " non trouvé");
            return;
        }

        StockMovement movement = new StockMovement();
        movement.setType(MovementTypeEnum.IN);
        movement.setCreationDatetime(Instant.now().minusSeconds(3600));

        StockValue value = new StockValue();
        value.setQuantity(quantity);
        value.setUnit(unit);
        movement.setValue(value);

        // Ajout en mémoire
        List<StockMovement> movements = ing.getStockMovementList();
        if (movements == null) {
            movements = new ArrayList<>();
            ing.setStockMovementList(movements);
        }
        movements.add(movement);

        // Sauvegarde
        dataRetriever.saveIngredient(ing);

        // RECHARGE l'ingrédient pour avoir les données fraîches
        ing = dataRetriever.findIngredientById(ingredientId);

        // Vérification immédiate
        StockValue stockAfter = ing.getStockValueAt(Instant.now());
        System.out.println("Après ajout → Stock de " + ing.getName() + " : " +
                (stockAfter != null ? stockAfter.getQuantity() + " " + stockAfter.getUnit() : "NULL"));
    }

    private Order createSimpleOrder() {
        Order order = new Order();
        order.setReference("ORD-TEST-" + System.currentTimeMillis());
        order.setCreationDatetime(Instant.now());
        order.setType(OrderType.EAT_IN);
        order.setStatus(OrderStatus.CREATED);

        Dish salade = dataRetriever.findDishById(1);
        Dish poulet = dataRetriever.findDishById(2);

        DishOrder line1 = new DishOrder();
        line1.setDish(salade);
        line1.setQuantity(1);   // ← 1 salade → 0.2 kg laitue + 0.15 kg tomate (doit passer avec 5 kg ajoutés)

        DishOrder line2 = new DishOrder();
        line2.setDish(poulet);
        line2.setQuantity(1);   // 1 poulet → 1 kg poulet

        order.setDishOrderList(List.of(line1, line2));

        return dataRetriever.saveOrder(order);
    }

    private Order createTakeAwayOrder() {
        Order order = new Order();
        order.setReference("TA-" + System.currentTimeMillis());
        order.setCreationDatetime(Instant.now());
        order.setType(OrderType.TAKE_AWAY);
        order.setStatus(OrderStatus.CREATED);

        Dish dessert = dataRetriever.findDishById(4);

        DishOrder line = new DishOrder();
        line.setDish(dessert);
        line.setQuantity(1);   // ← 1 gâteau → 0.3 kg chocolat + 0.2 kg beurre (doit passer)

        order.setDishOrderList(List.of(line));

        return dataRetriever.saveOrder(order);
    }

    private void printOrderDetails(Order order) {
        if (order == null) {
            System.out.println("Order is null");
            return;
        }

        System.out.println("ID              : " + order.getId());
        System.out.println("Reference       : " + order.getReference());
        System.out.println("Created at      : " + order.getCreationDatetime());
        System.out.println("Type            : " + order.getType());
        System.out.println("Status          : " + order.getStatus());
        System.out.println("Number of lines : " +
                (order.getDishOrderList() != null ? order.getDishOrderList().size() : 0));

        if (order.getDishOrderList() != null && !order.getDishOrderList().isEmpty()) {
            System.out.println("Order lines:");
            for (DishOrder line : order.getDishOrderList()) {
                Dish dish = line.getDish();
                System.out.printf("  - %d × %s (dish id %d)%n",
                        line.getQuantity(),
                        dish != null ? dish.getName() : "unknown dish",
                        dish != null ? dish.getId() : "?");
            }
        }

        System.out.println("----------------------------------------");
    }

    public static void main(String[] args) {
        DBConnection dbConnection = new DBConnection();
        DataRetriever retriever = new DataRetriever(dbConnection);

        Main tester = new Main(retriever);
        tester.runTests();
    }
}