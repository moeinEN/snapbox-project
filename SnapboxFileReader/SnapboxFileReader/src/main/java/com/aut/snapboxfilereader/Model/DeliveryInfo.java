package com.aut.snapboxfilereader.Model;


import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class DeliveryInfo {
    private int id_delivery;
    private double lat;
    private double lng;
    private long timestamp;
}
