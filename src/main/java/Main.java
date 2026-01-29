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

        // Optionnel : afficher le stock actuel après ajout (pour debug)
        // printCurrentStock(1);  // Laitue
        // printCurrentStock(2);  // Tomate
        // printCurrentStock(3);  // Poulet

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
        // Ajout de stock pour TOUS les ingrédients utilisés dans les tests
        addStockToIngredient(1, 10.0, Unit.KG);  // Laitue (salade)
        addStockToIngredient(2, 10.0, Unit.KG);  // Tomate (salade)
        addStockToIngredient(3, 10.0, Unit.KG);  // Poulet (plat principal)
        addStockToIngredient(4, 10.0, Unit.KG);  // Chocolat (gâteau du test 6)

        System.out.println("→ Stock ajouté : 10 kg pour chaque ingrédient clé");
        System.out.println("→ Les commandes devraient maintenant passer la vérification stock.");
    }

    private void addStockToIngredient(int ingredientId, double quantity, Unit unit) {
        Ingredient ing = dataRetriever.findIngredientById(ingredientId);
        if (ing == null) {
            System.out.println("Ingredient id " + ingredientId + " non trouvé → impossible d'ajouter du stock");
            return;
        }

        StockMovement movement = new StockMovement();
        movement.setType(MovementTypeEnum.IN);
        movement.setCreationDatetime(Instant.now().minusSeconds(3600)); // il y a 1 heure

        StockValue value = new StockValue();
        value.setQuantity(quantity);
        value.setUnit(unit);
        movement.setValue(value);

        List<StockMovement> movements = ing.getStockMovementList();
        if (movements == null) {
            movements = new ArrayList<>();
            ing.setStockMovementList(movements);
        }
        movements.add(movement);

        // Sauvegarde
        dataRetriever.saveIngredient(ing);
        System.out.println("→ Ajouté " + quantity + " " + unit + " à " + ing.getName());
    }

    // Optionnel : pour debug rapide du stock actuel
    private void printCurrentStock(int ingredientId) {
        Ingredient ing = dataRetriever.findIngredientById(ingredientId);
        if (ing != null) {
            StockValue stock = ing.getStockValueAt(Instant.now());
            System.out.println("Stock actuel de " + ing.getName() + " : " +
                    (stock != null ? stock.getQuantity() + " " + stock.getUnit() : "Aucun stock"));
        }
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
        line1.setQuantity(2);

        DishOrder line2 = new DishOrder();
        line2.setDish(poulet);
        line2.setQuantity(1);

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
        line.setQuantity(3);

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