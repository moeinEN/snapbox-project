package com.aut.snapbox.Model;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DeliveryInfo {
    private int id_delivery;
    private double lat;
    private double lng;
    private long timestamp;
}
