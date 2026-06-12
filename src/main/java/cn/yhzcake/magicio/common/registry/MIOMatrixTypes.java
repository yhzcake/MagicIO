package cn.yhzcake.magicio.common.registry;

import cn.yhzcake.magicio.MagicIO;
import cn.yhzcake.magicio.common.sys.matrix.MatrixType;
import net.minecraft.core.Holder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class MIOMatrixTypes {
    public static final DeferredRegister<MatrixType> MATRIX_TYPES = DeferredRegister.create(MIORegistries.MIOKeys.MATRIX_TYPE_REGISTRY_KEY, MagicIO.MOD_ID);

    //默认应为grid，暂时这样写
    public static final Holder<MatrixType> DEFAULT = MATRIX_TYPES.register("default", () -> new MatrixType(MatrixType.Properties.create()));

}
