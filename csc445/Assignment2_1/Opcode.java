package Assignment2_1;

import java.util.Optional;

public enum Opcode {
    RRQ(1),
    WRQ(2),
    DATA(3),
    ACK(4),
    ERROR(5);

    public final int opc;

    Opcode(int opc) {
        this.opc = opc;
    }

    public static Optional<Opcode> getOpcode(int opc) {
        for (Opcode op : values()) {
            if (op.opc == opc) {
                return Optional.of(op);
            }
        }
        return Optional.empty();
    }
}
