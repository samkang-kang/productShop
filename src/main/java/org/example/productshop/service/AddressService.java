package org.example.productshop.service;

import org.example.productshop.dao.AddressDao;
import org.example.productshop.entity.Address;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AddressService {

    private final AddressDao addressDao;

    public AddressService(AddressDao addressDao) {
        this.addressDao = addressDao;
    }

    @Transactional
    public long create(Address address) {
        // 若這筆是預設地址，先把同會員其他地址 is_default 歸 0
        if (Boolean.TRUE.equals(address.getIsDefault())) {
            addressDao.clearDefaultByMemberId(address.getMemberId());
        }
        return addressDao.insert(address);
    }

    public List<Address> listMy(int memberId) {
        return addressDao.listByMemberId(memberId);
    }

    @Transactional
    public boolean setDefault(int memberId, long addressId) {
        return addressDao.setDefaultById(memberId, addressId) > 0;
    }

    @Transactional
    public boolean deleteMy(int memberId, long addressId) {
        return addressDao.deleteByIdAndMember(addressId, memberId) > 0;
    }
}