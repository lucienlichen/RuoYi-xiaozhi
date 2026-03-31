import { defineStore } from 'pinia'
import { listEquipment } from '@/api/intellect/equipment'
import { listPartition } from '@/api/intellect/partition'

const useEquipmentStore = defineStore('equipment', {
  state: () => ({
    partitionsWithEquip: [],
    selectedEquipment: null,
    equipSearch: '',
    loaded: false,
  }),
  getters: {
    filteredPartitions(state) {
      const kw = state.equipSearch.toLowerCase()
      return state.partitionsWithEquip
        .map(p => ({
          ...p,
          equipments: p.equipments.filter(e => !kw || e.equipmentName.toLowerCase().includes(kw))
        }))
        .filter(p => !kw || p.equipments.length > 0)
    }
  },
  actions: {
    async loadEquipments() {
      const [partRes, equipRes] = await Promise.all([
        listPartition({}),
        listEquipment({ pageNum: 1, pageSize: 999 })
      ])
      const parts = partRes.data || []
      const equips = equipRes.rows || []
      this.partitionsWithEquip = parts.map(p => ({
        ...p,
        equipments: equips.filter(e => e.partitionId === p.id)
      }))
      const unassigned = equips.filter(e => !parts.some(p => p.id === e.partitionId))
      if (unassigned.length > 0) {
        this.partitionsWithEquip.push({ id: 0, partitionName: '未分配', equipments: unassigned })
      }
      this.loaded = true
    },
    selectEquipment(eq) {
      this.selectedEquipment = eq
    },
  }
})

export default useEquipmentStore
