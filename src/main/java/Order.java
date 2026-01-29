import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class Order {
    private Integer id;
    private String reference;
    private Instant creationDatetime;
    private List<DishOrder> dishOrderList;
    private OrderType type;
    private OrderStatus status;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public Instant getCreationDatetime() {
        return creationDatetime;
    }

    public void setCreationDatetime(Instant creationDatetime) {
        this.creationDatetime = creationDatetime;
    }

    public List<DishOrder> getDishOrderList() {
        return dishOrderList;
    }

    public void setDishOrderList(List<DishOrder> dishOrderList) {
        this.dishOrderList = dishOrderList;
    }

    @Override
    public String toString() {
        return "Order{" +
                "id=" + id +
                ", reference='" + reference + '\'' +
                ", creationDatetime=" + creationDatetime +
                ", dishOrderList=" + dishOrderList +
                '}';
    }

    Double getTotalAmountWithoutVat() {
        throw new RuntimeException("Not implemented");
    }

    Double getTotalAmountWithVat() {
        throw new RuntimeException("Not implemented");
    }


    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Order order)) return false;
        return Objects.equals(id, order.id) && Objects.equals(reference, order.reference) && Objects.equals(creationDatetime, order.creationDatetime) && Objects.equals(dishOrderList, order.dishOrderList);
    }

    public OrderType getType() {
        return type;
    }

    public void setType(OrderType type) {
        this.type = type;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Order order)) return false;
        return Objects.equals(getId(), order.getId()) && Objects.equals(getReference(), order.getReference()) && Objects.equals(getCreationDatetime(), order.getCreationDatetime()) && Objects.equals(getDishOrderList(), order.getDishOrderList()) && getType() == order.getType() && getStatus() == order.getStatus();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getReference(), getCreationDatetime(), getDishOrderList(), getType(), getStatus());
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Order{");
        sb.append("id=").append(id);
        sb.append(", reference='").append(reference).append('\'');
        sb.append(", creationDatetime=").append(creationDatetime);
        sb.append(", dishOrderList=").append(dishOrderList);
        sb.append(", type=").append(type);
        sb.append(", status=").append(status);
        sb.append('}');
        return sb.toString();
    }
}
