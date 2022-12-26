package com.cburch.logisim.std.wiring;

import static com.cburch.logisim.std.Strings.S;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.GraphicsUtil;

public class RiscVDecoder extends InstanceFactory {
    public static final String _ID = "RISC_V_Decoder";

    private static final int MAX_LEN = 31;
    private static final Color DEFAULT_COLOR = Color.BLUE;
    private static final Color DEFAULT_BACKGROUND = Color.WHITE;

    private static final Attribute<Color> ATTR_COLOR = Attributes.forColor("color", S.getter("riscVColor"));
    private static final Attribute<Color> ATTR_BACKGROUND = Attributes.forColor("bg", S.getter("riscVBackground"));

    private static final Map<Integer, String> CSR_TAB = new HashMap<>(Map.ofEntries(
        Map.entry(0x000, "ustatus"),
        Map.entry(0x004, "uie"),
        Map.entry(0x005, "utvec"),
        Map.entry(0x040, "uscratch"),
        Map.entry(0x041, "uepc"),
        Map.entry(0x042, "ucause"),
        Map.entry(0x043, "ubadaddr"),
        Map.entry(0x044, "uip"),
        Map.entry(0x001, "fflags"),
        Map.entry(0x002, "frm"),
        Map.entry(0x003, "fcsr"),
        Map.entry(0xc00, "cycle"),
        Map.entry(0xc01, "time"),
        Map.entry(0xc02, "instret"),
        Map.entry(0xc80, "cycleh"),
        Map.entry(0xc81, "timeh"),
        Map.entry(0xc82, "instreth"),
        Map.entry(0x100, "sstatus"),
        Map.entry(0x102, "sedeleg"),
        Map.entry(0x103, "sideleg"),
        Map.entry(0x104, "sie"),
        Map.entry(0x105, "stvec"),
        Map.entry(0x140, "sscratch"),
        Map.entry(0x141, "sepc"),
        Map.entry(0x142, "scause"),
        Map.entry(0x143, "sbadaddr"),
        Map.entry(0x144, "sip"),
        Map.entry(0x180, "sptbr"),
        Map.entry(0x200, "hstatus"),
        Map.entry(0x202, "hedeleg"),
        Map.entry(0x203, "hideleg"),
        Map.entry(0x204, "hie"),
        Map.entry(0x205, "htvec"),
        Map.entry(0x240, "hscratch"),
        Map.entry(0x241, "hepc"),
        Map.entry(0x242, "hcause"),
        Map.entry(0x243, "hbadaddr"),
        Map.entry(0x244, "hip"),
        Map.entry(0xf11, "mvendorid"),
        Map.entry(0xf12, "marchid"),
        Map.entry(0xf13, "mimpid"),
        Map.entry(0xf14, "mhartid"),
        Map.entry(0x300, "mstatus"),
        Map.entry(0x301, "misa"),
        Map.entry(0x302, "medeleg"),
        Map.entry(0x303, "mideleg"),
        Map.entry(0x304, "mie"),
        Map.entry(0x305, "mtvec"),
        Map.entry(0x340, "mscratch"),
        Map.entry(0x341, "mepc"),
        Map.entry(0x342, "mcause"),
        Map.entry(0x343, "mbadaddr"),
        Map.entry(0x344, "mip"),
        Map.entry(0x380, "mbase"),
        Map.entry(0x381, "mbound"),
        Map.entry(0x382, "mibase"),
        Map.entry(0x383, "mibound"),
        Map.entry(0x384, "mdbase"),
        Map.entry(0x385, "mdbound"),
        Map.entry(0xb00, "mcycle"),
        Map.entry(0xb02, "minstret"),
        Map.entry(0xb80, "mcycleh"),
        Map.entry(0xb82, "minstreth"),
        Map.entry(0x320, "mucounteren"),
        Map.entry(0x321, "mscounteren"),
        Map.entry(0x322, "mhcounteren"),
        Map.entry(0x7a0, "tselect"),
        Map.entry(0x7a1, "tdata1"),
        Map.entry(0x7a2, "tdata2"),
        Map.entry(0x7a3, "tdata3"),
        Map.entry(0x7b0, "dcsr"),
        Map.entry(0x7b1, "dpc"),
        Map.entry(0x7b2, "dscratch")
    ));

    static {
        for (int i = 3; i < 32; i++) {
            CSR_TAB.put(0x320 + i, "mhpmevent" + i);
            CSR_TAB.put(0xb00 + i, "mhpmcounter" + i);
            CSR_TAB.put(0xb80 + i, "mhpmcounter" + i + "h");
            CSR_TAB.put(0xc00 + i, "hpmcounter" + i);
            CSR_TAB.put(0xc80 + i, "hpmcounter" + i + "h");
        }
    }

    private static final class Instr {
        private final long instr;
        private final long instr_30_25;
        private final long instr_24_21;
        private final long instr_20;
        private final long instr_19_12;
        private final long instr_11_8;
        private final long instr_7;

        public Instr(long instr) {
            this.instr       = instr;
            this.instr_30_25 = (instr >> 25) & 0b111111;
            this.instr_24_21 = (instr >> 21) & 0b1111;
            this.instr_20    = (instr >> 20) & 0b1;
            this.instr_19_12 = (instr >> 12) & 0b11111111;
            this.instr_11_8  = (instr >>  8) & 0b1111;
            this.instr_7     = (instr >>  7) & 0b1;
        }

        private int withSign(int bits, long val) {
            if ((instr & 0x80000000L) == 0) {
                return (int)val;
            } else {
                return (int)((((1 << bits) - 1) << (32 - bits)) | val);
            }
        }

        public int oplen() {
            return (int)(instr & 0b1111111);
        }

        public int rd() {
            return (int)(instr >> 7) & 0b11111;
        }

        public int rs1() {
            return (int)(instr >> 15) & 0b11111;
        }

        public int rs2() {
            return (int)(instr >> 20) & 0b11111;
        }

        public int imm_i() {
            return withSign(21, (instr_30_25 << 5) | (instr_24_21 << 1) | instr_20);
        }

        public int imm_s() {
            return withSign(21, (instr_30_25 << 5) | (instr_11_8 << 1) | instr_7);
        }

        public int imm_b() {
            return withSign(20, (instr_7 << 11) | (instr_30_25 << 5) | (instr_11_8  << 1));
        }

        public int imm_u() {
            return (int)(instr & 0xfffff000L);
        }

        public int imm_j() {
            return withSign(12, (instr_19_12 << 12) | (instr_20 << 11) | (instr_30_25 << 5) | (instr_24_21 << 1));
        }

        public int funct3() {
            return (int)(instr >> 12) & 0b111;
        }

        public int funct7() {
            return (int)(instr >> 25) & 0b1111111;
        }

        public int funct7_rs2() {
            return (int)(instr >> 20) & 0b111111111111;
        }
    }

    private static final class StateData implements InstanceData, Cloneable {
        public long instr = 0L;
        public String decoded = null;

        private String pcrel(int rel) {
            if (rel >= 0) {
                return String.format(".+0x%08x", rel);
            } else {
                return String.format(".-0x%08x", -rel);
            }
        }

        private String csrid(int csr) {
            final var ret = CSR_TAB.get(csr);
            if (ret != null) {
                return ret;
            } else {
                return String.format("$0x%03x", csr);
            }
        }

        private void decodeLUI(Instr p) {
            decoded = String.format("lui      x%d, %#010x", p.rd(), p.imm_u() & 0xffffffffL);
        }

        private void decodeAUIPC(Instr p) {
            decoded = String.format("auipc    x%d, %#010x", p.rd(), p.imm_u() & 0xffffffffL);
        }

        private void decodeJAL(Instr p) {
            decoded = String.format("jal      x%d, %s", p.rd(), pcrel(p.imm_j()));
        }

        private void decodeJALR(Instr p) {
            if (p.funct3() == 0) {
                decoded = String.format("jalr     x%d, %d(x%d)", p.rd(), p.imm_i(), p.rs1());
            }
        }

        private void decodeBranch(Instr p) {
            switch (p.funct3()) {
                case 0b000: decoded = String.format("beq      x%d, x%d, %s", p.rs1(), p.rs2(), pcrel(p.imm_b())); break;
                case 0b001: decoded = String.format("bne      x%d, x%d, %s", p.rs1(), p.rs2(), pcrel(p.imm_b())); break;
                case 0b100: decoded = String.format("blt      x%d, x%d, %s", p.rs1(), p.rs2(), pcrel(p.imm_b())); break;
                case 0b101: decoded = String.format("bge      x%d, x%d, %s", p.rs1(), p.rs2(), pcrel(p.imm_b())); break;
                case 0b110: decoded = String.format("bltu     x%d, x%d, %s", p.rs1(), p.rs2(), pcrel(p.imm_b())); break;
                case 0b111: decoded = String.format("bgeu     x%d, x%d, %s", p.rs1(), p.rs2(), pcrel(p.imm_b())); break;
            }
        }

        private void decodeLoad(Instr p) {
            switch (p.funct3()) {
                case 0b000: decoded = String.format("lb       x%d, %d(x%d)", p.rd(), p.imm_i(), p.rs1()); break;
                case 0b001: decoded = String.format("lh       x%d, %d(x%d)", p.rd(), p.imm_i(), p.rs1()); break;
                case 0b010: decoded = String.format("lw       x%d, %d(x%d)", p.rd(), p.imm_i(), p.rs1()); break;
                case 0b100: decoded = String.format("lbu      x%d, %d(x%d)", p.rd(), p.imm_i(), p.rs1()); break;
                case 0b101: decoded = String.format("lhu      x%d, %d(x%d)", p.rd(), p.imm_i(), p.rs1()); break;
            }
        }

        private void decodeStore(Instr p) {
            switch (p.funct3()) {
                case 0b000: decoded = String.format("sb       x%d, %d(x%d)", p.rs2(), p.imm_s(), p.rs1()); break;
                case 0b001: decoded = String.format("sh       x%d, %d(x%d)", p.rs2(), p.imm_s(), p.rs1()); break;
                case 0b010: decoded = String.format("sw       x%d, %d(x%d)", p.rs2(), p.imm_s(), p.rs1()); break;
            }
        }

        private void decodeOpImm(Instr p) {
            switch (p.funct3()) {
                case 0b000: decoded = String.format("addi     x%d, x%d, %d", p.rd(), p.rs1(), p.imm_i()); break;
                case 0b010: decoded = String.format("slti     x%d, x%d, %d", p.rd(), p.rs1(), p.imm_i()); break;
                case 0b011: decoded = String.format("sltiu    x%d, x%d, %d", p.rd(), p.rs1(), p.imm_i()); break;
                case 0b100: decoded = String.format("xori     x%d, x%d, %d", p.rd(), p.rs1(), p.imm_i()); break;
                case 0b110: decoded = String.format("ori      x%d, x%d, %d", p.rd(), p.rs1(), p.imm_i()); break;
                case 0b111: decoded = String.format("andi     x%d, x%d, %d", p.rd(), p.rs1(), p.imm_i()); break;
                case 0b001: decoded = String.format("slli     x%d, x%d, %d", p.rd(), p.rs1(), p.rs2()); break;
                case 0b101: {
                    switch (p.funct7()) {
                        case 0b0000000: decoded = String.format("srli     x%d, x%d, %d", p.rd(), p.rs1(), p.rs2()); break;
                        case 0b0100000: decoded = String.format("srai     x%d, x%d, %d", p.rd(), p.rs1(), p.rs2()); break;
                    }
                    break;
                }
            }
        }

        private void decodeOp(Instr p) {
            switch (p.funct7()) {
                case 0b0000000: {
                    switch (p.funct3()) {
                        case 0b000: decoded = String.format("add      x%d, x%d, x%d", p.rd(), p.rs1(), p.rs2()); break;
                        case 0b001: decoded = String.format("sll      x%d, x%d, x%d", p.rd(), p.rs1(), p.rs2()); break;
                        case 0b010: decoded = String.format("slt      x%d, x%d, x%d", p.rd(), p.rs1(), p.rs2()); break;
                        case 0b011: decoded = String.format("sltu     x%d, x%d, x%d", p.rd(), p.rs1(), p.rs2()); break;
                        case 0b100: decoded = String.format("xor      x%d, x%d, x%d", p.rd(), p.rs1(), p.rs2()); break;
                        case 0b101: decoded = String.format("srl      x%d, x%d, x%d", p.rd(), p.rs1(), p.rs2()); break;
                        case 0b110: decoded = String.format("or       x%d, x%d, x%d", p.rd(), p.rs1(), p.rs2()); break;
                        case 0b111: decoded = String.format("and      x%d, x%d, x%d", p.rd(), p.rs1(), p.rs2()); break;
                    }
                    break;
                }
                case 0b0000001: {
                    switch (p.funct3()) {
                        case 0b000: decoded = String.format("mul      x%d, x%d, x%d", p.rd(), p.rs1(), p.rs2()); break;
                        case 0b001: decoded = String.format("mulh     x%d, x%d, x%d", p.rd(), p.rs1(), p.rs2()); break;
                        case 0b010: decoded = String.format("mulhsu   x%d, x%d, x%d", p.rd(), p.rs1(), p.rs2()); break;
                        case 0b011: decoded = String.format("mulhu    x%d, x%d, x%d", p.rd(), p.rs1(), p.rs2()); break;
                        case 0b100: decoded = String.format("div      x%d, x%d, x%d", p.rd(), p.rs1(), p.rs2()); break;
                        case 0b101: decoded = String.format("divu     x%d, x%d, x%d", p.rd(), p.rs1(), p.rs2()); break;
                        case 0b110: decoded = String.format("rem      x%d, x%d, x%d", p.rd(), p.rs1(), p.rs2()); break;
                        case 0b111: decoded = String.format("remu     x%d, x%d, x%d", p.rd(), p.rs1(), p.rs2()); break;
                    }
                    break;
                }
                case 0b0100000: {
                    switch (p.funct3()) {
                        case 0b000: decoded = String.format("sub      x%d, x%d, x%d", p.rd(), p.rs1(), p.rs2()); break;
                        case 0b101: decoded = String.format("sra      x%d, x%d, x%d", p.rd(), p.rs1(), p.rs2()); break;
                    }
                    break;
                }
            }
        }

        private void decodeMiscMem(Instr p) {
            switch (p.funct3()) {
                case 0b000: decoded = "fence"; break;
                case 0b001: decoded = "fence.i"; break;
            }
        }

        private void decodeSystem(Instr p) {
            switch (p.funct3()) {
                case 0b000: {
                    if (p.rd() == 0 && p.rs1() == 0) {
                        switch (p.funct7_rs2()) {
                            case 0b000000000000: decoded = "ecall"; break;
                            case 0b000000000001: decoded = "ebreak"; break;
                            case 0b000000000010: decoded = "uret"; break;
                            case 0b000100000010: decoded = "sret"; break;
                            case 0b001100000010: decoded = "mret"; break;
                            case 0b000100000101: decoded = "wfi"; break;
                        }
                    }
                    break;
                }
                case 0b001: decoded = String.format("csrrw    x%d, %s, x%d", p.rd(), csrid(p.imm_i()), p.rs1()); break;
                case 0b010: decoded = String.format("csrrs    x%d, %s, x%d", p.rd(), csrid(p.imm_i()), p.rs1()); break;
                case 0b011: decoded = String.format("csrrc    x%d, %s, x%d", p.rd(), csrid(p.imm_i()), p.rs1()); break;
                case 0b101: decoded = String.format("csrrwi   x%d, %s, 0x%08x", p.rd(), csrid(p.imm_i()), p.rs1()); break;
                case 0b110: decoded = String.format("csrrsi   x%d, %s, 0x%08x", p.rd(), csrid(p.imm_i()), p.rs1()); break;
                case 0b111: decoded = String.format("csrrci   x%d, %s, 0x%08x", p.rd(), csrid(p.imm_i()), p.rs1()); break;
            }
        }

        private void decode(Instr p) {
            switch (p.oplen()) {
                case 0b0110111: decodeLUI     (p); break;
                case 0b0010111: decodeAUIPC   (p); break;
                case 0b1101111: decodeJAL     (p); break;
                case 0b1100111: decodeJALR    (p); break;
                case 0b1100011: decodeBranch  (p); break;
                case 0b0000011: decodeLoad    (p); break;
                case 0b0100011: decodeStore   (p); break;
                case 0b0010011: decodeOpImm   (p); break;
                case 0b0110011: decodeOp      (p); break;
                case 0b0001111: decodeMiscMem (p); break;
                case 0b1110011: decodeSystem  (p); break;
            }
        }

        public void update(long instr) {
            this.instr = instr & 0xffffffffL;
            this.decoded = null;
            decode(new Instr(instr));
        }

        @Override
        public Object clone() {
            try {
                return super.clone();
            } catch (CloneNotSupportedException e) {
                return null;
            }
        }
    }

    public RiscVDecoder() {
        super(_ID, S.getter("riscVDecoder"));
        setAttributes(
            new Attribute[] {
                StdAttr.FACING,
                ATTR_COLOR,
                ATTR_BACKGROUND,
                StdAttr.LABEL,
                StdAttr.LABEL_LOC,
                StdAttr.LABEL_FONT,
                StdAttr.LABEL_COLOR,
                StdAttr.LABEL_VISIBILITY,
            },
            new Object[] {
                Direction.WEST,
                DEFAULT_COLOR,
                DEFAULT_BACKGROUND,
                "",
                Direction.NORTH,
                StdAttr.DEFAULT_LABEL_FONT,
                StdAttr.DEFAULT_LABEL_COLOR,
                true,
            }
        );
        setIconName("rvdec.gif");
        setPorts(new Port[] { new Port(0, 0, Port.INPUT, 32) });
    }

    private StateData resolveStateData(InstanceState state) {
        var data = (StateData)state.getData();
        if (data == null) {
            data = new StateData();
            state.setData(data);
        }
        return data;
    }

    @Override
    protected void configureNewInstance(Instance instance) {
        instance.addAttributeListener();
        instance.computeLabelTextField(Instance.AVOID_LEFT);
    }

    @Override
    public Bounds getOffsetBounds(AttributeSet attrs) {
        final var h = 30;
        final var w = Pin.DIGIT_WIDTH * MAX_LEN + 20;
        final var dir = attrs.getValue(StdAttr.FACING);
        if (dir == Direction.WEST) {
            return Bounds.create(0, -h / 2, w, h);
        } else if (dir == Direction.EAST) {
            return Bounds.create(-w, -h / 2, w, h);
        } else if (dir == Direction.NORTH) {
            return Bounds.create(-w / 2, 0, w, h);
        } else {
            return Bounds.create(-w / 2, -h, w, h);
        }
    }

    @Override
    protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
        if (attr == StdAttr.FACING) {
            instance.recomputeBounds();
        }
        if (attr == StdAttr.FACING || attr == StdAttr.LABEL_LOC) {
            instance.computeLabelTextField(Instance.AVOID_LEFT);
        }
    }

    @Override
    public void paintGhost(InstancePainter painter) {
        final var g = painter.getGraphics();
        final var bds = painter.getBounds();
        GraphicsUtil.switchToWidth(g, 2);
        g.drawRoundRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight(), 8, 8);
    }

    @Override
    public void paintInstance(InstancePainter painter) {
        final var g = painter.getGraphics();
        final var bds = painter.getBounds();
        final var state = resolveStateData(painter);
        g.setColor(painter.getAttributeValue(ATTR_BACKGROUND));
        g.fillRoundRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight(), 8, 8);
        GraphicsUtil.switchToWidth(g, 2);
        g.setColor(Color.BLACK);
        g.drawRoundRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight(), 8, 8);
        GraphicsUtil.switchToWidth(g, 1);
        painter.drawLabel();
        painter.drawPorts();
        g.setFont(Pin.DEFAULT_FONT);
        final var x = bds.getX() + 10;
        final var y = bds.getY() + (bds.getHeight() + g.getFontMetrics().getAscent()) / 2;
        if (state.decoded == null) {
            g.setColor(Color.RED);
            g.drawString(S.get("riscVInvalidInstr", state.instr), x, y);
        } else {
            g.setColor(painter.getAttributeValue(ATTR_COLOR));
            g.drawString(state.decoded, x, y);
        }
    }

    @Override
    public void propagate(InstanceState state) {
        final var data = resolveStateData(state);
        final var value = state.getPortValue(0).toLongValue();
        if (data.instr != value) {
            data.update(value);
        }
    }
}
