import java.sql.*;
import java.time.Instant;
import java.util.*;


public class DataRetriever {

    private final DBConnection dbConnection;

    public DataRetriever(DBConnection dbConnection) {
        if (dbConnection == null) {
            throw new IllegalArgumentException("DBConnection ne peut pas être null");
        }
        this.dbConnection = dbConnection;
    }

    public Order findOrderByReference(String reference) {
        String sql = """
                SELECT id, reference, creation_datetime, order_type, status
                FROM "order"
                WHERE reference = ?
                """;

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, reference);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Order order = new Order();
                    Integer idOrder = rs.getInt("id");
                    order.setId(idOrder);
                    order.setReference(rs.getString("reference"));
                    order.setCreationDatetime(rs.getTimestamp("creation_datetime").toInstant());
                    order.setType(OrderType.valueOf(rs.getString("order_type")));
                    order.setStatus(OrderStatus.valueOf(rs.getString("status")));
                    order.setDishOrderList(findDishOrderByIdOrder(idOrder, conn));
                    return order;
                }
                throw new RuntimeException("Order not found with reference " + reference);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recherche de la commande", e);
        }
    }

    private List<DishOrder> findDishOrderByIdOrder(Integer idOrder, Connection conn) throws SQLException {
        List<DishOrder> dishOrders = new ArrayList<>();

        String sql = """
                SELECT id, id_dish, quantity
                FROM dish_order
                WHERE id_order = ?
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idOrder);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Dish dish = findDishById(rs.getInt("id_dish"), conn);
                    DishOrder dishOrder = new DishOrder();
                    dishOrder.setId(rs.getInt("id"));
                    dishOrder.setQuantity(rs.getInt("quantity"));
                    dishOrder.setDish(dish);
                    dishOrders.add(dishOrder);
                }
            }
        }
        return dishOrders;
    }

    public Dish findDishById(Integer id) {
        try (Connection conn = dbConnection.getConnection()) {
            return findDishById(id, conn);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Dish findDishById(Integer id, Connection conn) throws SQLException {
        String sql = """
                SELECT id AS dish_id, name AS dish_name, dish_type, selling_price AS dish_price
                FROM dish
                WHERE id = ?
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Dish dish = new Dish();
                    dish.setId(rs.getInt("dish_id"));
                    dish.setName(rs.getString("dish_name"));
                    dish.setDishType(DishTypeEnum.valueOf(rs.getString("dish_type")));
                    dish.setPrice(rs.getObject("dish_price") == null ? null : rs.getDouble("dish_price"));
                    dish.setDishIngredients(findDishIngredientsByDishId(id, conn));
                    return dish;
                }
                throw new RuntimeException("Dish not found " + id);
            }
        }
    }

    private List<DishIngredient> findDishIngredientsByDishId(Integer dishId, Connection conn) throws SQLException {
        List<DishIngredient> ingredients = new ArrayList<>();

        String sql = """
                SELECT i.id, i.name, i.price, i.category, di.required_quantity, di.unit
                FROM ingredient i
                JOIN dish_ingredient di ON di.id_ingredient = i.id
                WHERE di.id_dish = ?
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, dishId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Ingredient ing = new Ingredient();
                    ing.setId(rs.getInt("id"));
                    ing.setName(rs.getString("name"));
                    ing.setPrice(rs.getDouble("price"));
                    ing.setCategory(CategoryEnum.valueOf(rs.getString("category")));

                    DishIngredient di = new DishIngredient();
                    di.setIngredient(ing);
                    di.setQuantity(rs.getDouble("required_quantity"));
                    di.setUnit(Unit.valueOf(rs.getString("unit")));

                    ingredients.add(di);
                }
            }
        }
        return ingredients;
    }

    public Ingredient saveIngredient(Ingredient toSave) {
        try (Connection conn = dbConnection.getConnection()) {
            conn.setAutoCommit(false);

            Integer id = upsertIngredient(toSave, conn);
            insertIngredientStockMovements(id, toSave.getStockMovementList(), conn);

            conn.commit();
            return findIngredientById(id, conn);
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la sauvegarde de l'ingrédient", e);
        }
    }

    private Integer upsertIngredient(Ingredient toSave, Connection conn) throws SQLException {
        String sql = """
                INSERT INTO ingredient (id, name, price, category)
                VALUES (?, ?, ?, ?::ingredient_category)
                ON CONFLICT (id) DO UPDATE
                SET name = EXCLUDED.name,
                    price = EXCLUDED.price,
                    category = EXCLUDED.category
                RETURNING id
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, toSave.getId(), Types.INTEGER);
            ps.setString(2, toSave.getName());
            ps.setObject(3, toSave.getPrice(), Types.DOUBLE);
            ps.setString(4, toSave.getCategory().name());

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private void insertIngredientStockMovements(Integer ingredientId, List<StockMovement> movements, Connection conn) throws SQLException {
        if (movements == null || movements.isEmpty()) return;

        String sql = """
                INSERT INTO stock_movement (id, id_ingredient, quantity, unit, type, creation_datetime)
                VALUES (nextval('stock_movement_id_seq'), ?, ?, ?::unit, ?::movement_type, ?)
                ON CONFLICT (id) DO NOTHING
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (StockMovement sm : movements) {
                ps.setInt(1, ingredientId);
                ps.setDouble(2, sm.getValue().getQuantity());
                ps.setString(3, sm.getValue().getUnit().name());
                ps.setString(4, sm.getType().name());
                ps.setTimestamp(5, Timestamp.from(sm.getCreationDatetime()));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public Ingredient findIngredientById(Integer id) {
        try (Connection conn = dbConnection.getConnection()) {
            return findIngredientById(id, conn);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Ingredient findIngredientById(Integer id, Connection conn) throws SQLException {
        String sql = """
                SELECT id, name, price, category
                FROM ingredient
                WHERE id = ?
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Ingredient(
                            rs.getInt("id"),
                            rs.getString("name"),
                            CategoryEnum.valueOf(rs.getString("category")),
                            rs.getDouble("price"),
                            findStockMovementsByIngredientId(id, conn)
                    );
                }
                throw new RuntimeException("Ingredient not found " + id);
            }
        }
    }

    private List<StockMovement> findStockMovementsByIngredientId(Integer id, Connection conn) throws SQLException {
        List<StockMovement> list = new ArrayList<>();

        String sql = """
                SELECT id, quantity, unit, type, creation_datetime
                FROM stock_movement
                WHERE id_ingredient = ?
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    StockMovement sm = new StockMovement();
                    sm.setId(rs.getInt("id"));
                    sm.setType(MovementTypeEnum.valueOf(rs.getString("type")));
                    sm.setCreationDatetime(rs.getTimestamp("creation_datetime").toInstant());

                    StockValue sv = new StockValue();
                    sv.setQuantity(rs.getDouble("quantity"));
                    sv.setUnit(Unit.valueOf(rs.getString("unit")));
                    sm.setValue(sv);

                    list.add(sm);
                }
            }
        }
        return list;
    }

    public Dish saveDish(Dish toSave) {
        try (Connection conn = dbConnection.getConnection()) {
            conn.setAutoCommit(false);

            Integer dishId = upsertDish(toSave, conn);
            detachIngredients(dishId, conn);
            attachIngredients(dishId, toSave.getDishIngredients(), conn);

            conn.commit();
            return findDishById(dishId, conn);
        } catch (SQLException e) {
            throw new RuntimeException("Erreur sauvegarde plat", e);
        }
    }

    private Integer upsertDish(Dish toSave, Connection conn) throws SQLException {
        String sql = """
                INSERT INTO dish (id, name, dish_type, selling_price)
                VALUES (?, ?, ?::dish_type, ?)
                ON CONFLICT (id) DO UPDATE
                SET name = EXCLUDED.name,
                    dish_type = EXCLUDED.dish_type,
                    selling_price = EXCLUDED.selling_price
                RETURNING id
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, toSave.getId(), Types.INTEGER);
            ps.setString(2, toSave.getName());
            ps.setString(3, toSave.getDishType().name());
            ps.setObject(4, toSave.getPrice(), Types.DOUBLE);

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private void detachIngredients(Integer dishId, Connection conn) throws SQLException {
        String sql = "DELETE FROM dish_ingredient WHERE id_dish = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, dishId);
            ps.executeUpdate();
        }
    }

    private void attachIngredients(Integer dishId, List<DishIngredient> ingredients, Connection conn) throws SQLException {
        if (ingredients == null || ingredients.isEmpty()) return;

        String sql = """
                INSERT INTO dish_ingredient (id, id_dish, id_ingredient, required_quantity, unit)
                VALUES (nextval('dish_ingredient_id_seq'), ?, ?, ?, ?::unit)
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (DishIngredient di : ingredients) {
                ps.setInt(1, dishId);
                ps.setInt(2, di.getIngredient().getId());
                ps.setDouble(3, di.getQuantity());
                ps.setString(4, di.getUnit().name());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public Order saveOrder(Order orderToSave) {
        if (orderToSave == null) {
            throw new IllegalArgumentException("La commande ne peut pas être null");
        }

        // Vérification stock
        checkStockSufficiency(orderToSave);

        try (Connection conn = dbConnection.getConnection()) {
            conn.setAutoCommit(false);

            // Vérification statut existant si update
            if (orderToSave.getId() != null) {
                Order existing = findOrderById(orderToSave.getId(), conn);
                if (existing != null && existing.getStatus() == OrderStatus.DELIVERED) {
                    throw new IllegalStateException("Une commande livrée (DELIVERED) ne peut plus être modifiée");
                }
            }

            Integer orderId = upsertOrder(orderToSave, conn);
            deleteDishOrders(orderId, conn);
            insertDishOrders(orderId, orderToSave.getDishOrderList(), conn);

            conn.commit();

            orderToSave.setId(orderId);
            return orderToSave;

        } catch (SQLException e) {
            throw new RuntimeException("Échec sauvegarde commande", e);
        }
    }

    private void checkStockSufficiency(Order order) {
        if (order.getDishOrderList() == null || order.getDishOrderList().isEmpty()) {
            return;
        }

        for (DishOrder line : order.getDishOrderList()) {
            Dish dish = line.getDish();
            if (dish == null || dish.getDishIngredients() == null) {
                continue;
            }

            int qtyWanted = line.getQuantity();

            for (DishIngredient di : dish.getDishIngredients()) {
                Ingredient ing = di.getIngredient();
                if (ing == null) continue;

                double needed = di.getQuantity() * qtyWanted;

                StockValue current = ing.getStockValueAt(Instant.now());

                double available = (current != null) ? current.getQuantity() : 0.0;
                Unit unit = (current != null && current.getUnit() != null) ? current.getUnit() : di.getUnit();

                if (available < needed) {
                    throw new RuntimeException(
                            String.format(
                                    "Stock insuffisant pour l'ingrédient '%s' " +
                                            "(besoin: %.2f %s, disponible: %.2f %s)",
                                    ing.getName(),
                                    needed, di.getUnit(),
                                    available, unit
                            )
                    );
                }
            }
        }
    }

    private Order findOrderById(Integer id, Connection conn) throws SQLException {
        String sql = """
                SELECT id, reference, creation_datetime, order_type, status
                FROM "order"
                WHERE id = ?
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Order o = new Order();
                    o.setId(rs.getInt("id"));
                    o.setReference(rs.getString("reference"));
                    o.setCreationDatetime(rs.getTimestamp("creation_datetime").toInstant());
                    o.setType(OrderType.valueOf(rs.getString("order_type")));
                    o.setStatus(OrderStatus.valueOf(rs.getString("status")));
                    return o;
                }
                return null;
            }
        }
    }

    private Integer upsertOrder(Order order, Connection conn) throws SQLException {
        String sql = """
                INSERT INTO "order" (id, reference, creation_datetime, order_type, status)
                VALUES (?, ?, ?, ?::order_type, ?::order_status)
                ON CONFLICT (id) DO UPDATE SET
                    reference = EXCLUDED.reference,
                    creation_datetime = EXCLUDED.creation_datetime,
                    order_type = EXCLUDED.order_type,
                    status = EXCLUDED.status
                RETURNING id
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, order.getId(), Types.INTEGER);
            ps.setString(2, order.getReference());
            ps.setTimestamp(3, Timestamp.from(order.getCreationDatetime()));
            ps.setString(4, order.getType().name());
            ps.setString(5, order.getStatus().name());

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private void deleteDishOrders(Integer orderId, Connection conn) throws SQLException {
        String sql = "DELETE FROM dish_order WHERE id_order = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.executeUpdate();
        }
    }

    private void insertDishOrders(Integer orderId, List<DishOrder> lines, Connection conn) throws SQLException {
        if (lines == null || lines.isEmpty()) return;

        String sql = """
                INSERT INTO dish_order (id_order, id_dish, quantity)
                VALUES (?, ?, ?)
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (DishOrder line : lines) {
                ps.setInt(1, orderId);
                ps.setInt(2, line.getDish().getId());
                ps.setInt(3, line.getQuantity());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public List<Ingredient> createIngredients(List<Ingredient> newIngredients) {
        if (newIngredients == null || newIngredients.isEmpty()) return List.of();

        List<Ingredient> saved = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection()) {
            conn.setAutoCommit(false);

            String sql = """
                    INSERT INTO ingredient (id, name, category, price)
                    VALUES (?, ?, ?::ingredient_category, ?)
                    RETURNING id
                    """;

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (Ingredient ing : newIngredients) {
                    ps.setObject(1, ing.getId(), Types.INTEGER);
                    ps.setString(2, ing.getName());
                    ps.setString(3, ing.getCategory().name());
                    ps.setObject(4, ing.getPrice(), Types.DOUBLE);

                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        ing.setId(rs.getInt(1));
                        saved.add(ing);
                    }
                }
            }
            conn.commit();
            return saved;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur création batch ingrédients", e);
        }
    }

    private String getSerialSequenceName(Connection conn, String tableName, String columnName) throws SQLException {
        String sql = "SELECT pg_get_serial_sequence(?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableName);
            ps.setString(2, columnName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        }
        return null;
    }

    private int getNextSerialValue(Connection conn, String tableName, String columnName) throws SQLException {
        String seq = getSerialSequenceName(conn, tableName, columnName);
        if (seq == null) {
            throw new IllegalArgumentException("Aucune séquence trouvée pour " + tableName + "." + columnName);
        }
        String sql = "SELECT nextval(?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, seq);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}