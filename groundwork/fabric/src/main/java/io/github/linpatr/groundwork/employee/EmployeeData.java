package io.github.linpatr.groundwork.employee;

import io.github.linpatr.groundwork.core.employee.EmployeeDirectory;
import io.github.linpatr.groundwork.core.employee.EmployeeRecord;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.time.Clock;
import java.time.Instant;

/** Persists the {@link EmployeeDirectory} in the world save ({@code data/groundwork_employees.dat}). */
public final class EmployeeData extends SavedData {
    private static final String FILE_ID = "groundwork_employees";
    private static final int FORMAT_VERSION = 1;

    private final EmployeeDirectory directory = new EmployeeDirectory(Clock.systemUTC(), this::setDirty);

    public static EmployeeData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(EmployeeData::new, EmployeeData::load, null), FILE_ID);
    }

    public EmployeeDirectory directory() {
        return directory;
    }

    private static EmployeeData load(CompoundTag tag, HolderLookup.Provider registries) {
        EmployeeData data = new EmployeeData();
        ListTag employees = tag.getList("Employees", Tag.TAG_COMPOUND);
        for (int i = 0; i < employees.size(); i++) {
            CompoundTag entry = employees.getCompound(i);
            data.directory.restore(new EmployeeRecord(
                    entry.getUUID("Player"),
                    entry.getInt("Number"),
                    Instant.ofEpochMilli(entry.getLong("HiredAt")),
                    entry.getInt("Clearance"),
                    entry.getInt("Constructed")));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag employees = new ListTag();
        for (EmployeeRecord record : directory.all()) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Player", record.playerId());
            entry.putInt("Number", record.employeeNumber());
            entry.putLong("HiredAt", record.hiredAt().toEpochMilli());
            entry.putInt("Clearance", record.clearance());
            entry.putInt("Constructed", record.buildingsConstructed());
            employees.add(entry);
        }
        tag.putInt("Version", FORMAT_VERSION);
        tag.put("Employees", employees);
        return tag;
    }
}
