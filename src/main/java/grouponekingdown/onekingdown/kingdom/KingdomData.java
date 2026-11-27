package grouponekingdown.onekingdown.kingdom;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class KingdomData {

    public static class WorldState {
        public int nextRoomId = 1;
        public List<Room> rooms = new ArrayList<>();
    }

    public static class Room {
        public int roomId;
        public int maxPlayers;
        public List<Kingdom> kingdoms = new ArrayList<>();

        public Room() {}

        public Room(int roomId, int maxPlayers) {
            this.roomId = roomId;
            this.maxPlayers = maxPlayers;
        }

        public boolean hasSpace() {
            return kingdoms.size() < maxPlayers;
        }
    }

    public static class Kingdom {
        public String ownerUuid;
        public String ownerName;
        public int roomId;
        public int slotIndex;
        public int centerX;
        public int centerY;
        public int centerZ;
        public int sideSize;
        public int level = 1;
        public int xp = 0;
        public int currentTier = 1;

        // Tropas e trabalhadores contratados pelo General
        public int guardsCount = 1;
        public int minersCount = 1;
        public int farmersCount = 1;

        public Kingdom() {}

        public Kingdom(UUID ownerUuid, String ownerName, int roomId, int slotIndex,
                       int centerX, int centerY, int centerZ, int sideSize) {
            this.ownerUuid = ownerUuid.toString();
            this.ownerName = ownerName;
            this.roomId = roomId;
            this.slotIndex = slotIndex;
            this.centerX = centerX;
            this.centerY = centerY;
            this.centerZ = centerZ;
            this.sideSize = sideSize;
            this.level = 1;
            this.xp = 0;
            this.currentTier = 1;
            this.guardsCount = 1;
            this.minersCount = 1;
            this.farmersCount = 1;
        }

        public int getMinX() {
            return centerX - (sideSize / 2);
        }

        public int getMaxX() {
            return getMinX() + sideSize - 1;
        }

        public int getMinZ() {
            return centerZ - (sideSize / 2);
        }

        public int getMaxZ() {
            return getMinZ() + sideSize - 1;
        }

        public BlockPos getCenterPos() {
            return new BlockPos(centerX, centerY, centerZ);
        }

        // Posicoes das construcoes secundarias dentro do territorio do reino
        public BlockPos getBarracksPos() {
            int offset = Math.max(22, (sideSize / 2) - 22);
            return new BlockPos(centerX + offset, centerY, centerZ);
        }

        public BlockPos getMinePos() {
            int offset = Math.max(22, (sideSize / 2) - 22);
            return new BlockPos(centerX - offset, centerY, centerZ);
        }

        public BlockPos getFarmPos() {
            int offset = Math.max(22, (sideSize / 2) - 22);
            return new BlockPos(centerX, centerY, centerZ - offset);
        }

        public int getMaxGuards() {
            return 3 + (currentTier * 2);
        }

        public int getMaxMiners() {
            return 2 + currentTier;
        }

        public int getMaxFarmers() {
            return 2 + currentTier;
        }

        public boolean contains(BlockPos pos) {
            return pos.getX() >= getMinX() && pos.getX() <= getMaxX()
                    && pos.getZ() >= getMinZ() && pos.getZ() <= getMaxZ();
        }

        public static int calculateTier(int level) {
            return (level / 10) + 1;
        }

        public boolean addXp(int amount, int xpPerLevel) {
            this.xp += amount;
            while (this.xp >= xpPerLevel) {
                this.xp -= xpPerLevel;
                this.level++;
            }
            int newTier = calculateTier(this.level);
            if (newTier > this.currentTier) {
                this.currentTier = newTier;
                return true;
            }
            return false;
        }
    }
}