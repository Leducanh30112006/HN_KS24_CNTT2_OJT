package com.quickshop.order.repository;

import com.quickshop.order.entity.Voucher;
import com.quickshop.order.entity.enums.VoucherStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    Optional<Voucher> findByCodeAndStatus(String code, VoucherStatus status);

    Optional<Voucher> findByCode(String code);

    /**
     * Tăng lượt sử dụng voucher một cách nguyên tử (Atomic Update) ở tầng CSDL.
     * Ngăn chặn hoàn toàn tình trạng overselling khi có nhiều request checkout đồng thời cạnh tranh usage_limit.
     *
     * @param id     ID voucher
     * @param status Trạng thái yêu cầu (ACTIVE)
     * @return Số bản ghi được cập nhật (1 nếu thành công, 0 nếu đã hết lượt dùng hoặc không còn ACTIVE)
     */
    @Modifying
    @Query("UPDATE Voucher v SET v.usedCount = v.usedCount + 1 " +
           "WHERE v.id = :id AND v.usedCount < v.usageLimit AND v.status = :status")
    int incrementUsageCountAtomic(@Param("id") Long id, @Param("status") VoucherStatus status);

    /**
     * Hoàn tác lượt sử dụng của voucher theo ID (Compensating Transaction khi Saga thất bại).
     *
     * @param id ID voucher
     * @return Số bản ghi được cập nhật (1 nếu hoàn tác thành công)
     */
    @Modifying
    @Query("UPDATE Voucher v SET v.usedCount = v.usedCount - 1 " +
           "WHERE v.id = :id AND v.usedCount > 0")
    int decrementUsageCountAtomic(@Param("id") Long id);

    /**
     * Hoàn tác lượt sử dụng của voucher theo Code (Compensating Transaction khi Saga thất bại).
     *
     * @param code Mã voucher
     * @return Số bản ghi được cập nhật (1 nếu hoàn tác thành công)
     */
    @Modifying
    @Query("UPDATE Voucher v SET v.usedCount = v.usedCount - 1 " +
           "WHERE v.code = :code AND v.usedCount > 0")
    int decrementUsageCountByCodeAtomic(@Param("code") String code);
}
