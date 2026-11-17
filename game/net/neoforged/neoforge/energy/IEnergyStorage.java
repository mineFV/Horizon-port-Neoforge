package net.neoforged.neoforge.energy;

/** The stand-in for NeoForge's IEnergyStorage: its methods (a mod's implement them). */
public interface IEnergyStorage {
    int receiveEnergy(int toReceive, boolean simulate);

    int extractEnergy(int toExtract, boolean simulate);

    int getEnergyStored();

    int getMaxEnergyStored();

    boolean canExtract();

    boolean canReceive();
}
