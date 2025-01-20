package me.nighter.smartSpawner.hooks.protections;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Towny {
    public static boolean IfPlayerHasResident(@NotNull UUID pUUID, @NotNull Location location){

        Resident resident = TownyAPI.getInstance().getResident(pUUID);
        if (resident == null) return false;

        try {
            Town town = resident.getTown();
        } catch (NotRegisteredException e) {
            // Log the exception and return false if the resident does not belong to any town
            // e.printStackTrace();
            return false;
        }
        return true;
    }
}
